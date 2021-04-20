package ir.cafebazaar.poolakey

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.os.IBinder
import androidx.fragment.app.Fragment
import com.android.vending.billing.IInAppBillingService
import ir.cafebazaar.poolakey.billing.BillingFunction
import ir.cafebazaar.poolakey.billing.consume.ConsumeFunctionRequest
import ir.cafebazaar.poolakey.billing.purchase.PurchaseFunctionRequest
import ir.cafebazaar.poolakey.billing.query.QueryFunctionRequest
import ir.cafebazaar.poolakey.billing.skudetail.SkuDetailFunctionRequest
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.constant.Billing
import ir.cafebazaar.poolakey.exception.BazaarNotFoundException
import ir.cafebazaar.poolakey.exception.DisconnectException
import ir.cafebazaar.poolakey.exception.IAPNotSupportedException
import ir.cafebazaar.poolakey.exception.SubsNotSupportedException
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration,
    private val backgroundThread: PoolakeyThread<Runnable>,
    private val purchaseFunction: BillingFunction<PurchaseFunctionRequest>,
    private val consumeFunction: BillingFunction<ConsumeFunctionRequest>,
    private val queryFunction: BillingFunction<QueryFunctionRequest>,
    private val skuDetailFunction: BillingFunction<SkuDetailFunctionRequest>
) : ServiceConnection {

    private var callback: ConnectionCallback? = null

    private var billingService: IInAppBillingService? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)
        Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf(
                thisIsTrue = {
                    context.packageManager.queryIntentServices(it, 0).isNullOrEmpty().not()
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(BazaarNotFoundException())
                }
            )
            ?.also {
                try {
                    context.bindService(it, this, Context.BIND_AUTO_CREATE)
                } catch (e: SecurityException) {
                    callback?.connectionFailed?.invoke(e)
                }
            }
        return requireNotNull(callback)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        IInAppBillingService.Stub.asInterface(service)
            ?.takeIf(
                thisIsTrue = {
                    isPurchaseTypeSupported(
                        purchaseType = PurchaseType.IN_APP,
                        inAppBillingService = it
                    )
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(IAPNotSupportedException())
                }
            )
            ?.takeIf(
                thisIsTrue = {
                    !paymentConfiguration.shouldSupportSubscription || isPurchaseTypeSupported(
                        purchaseType = PurchaseType.SUBSCRIPTION,
                        inAppBillingService = it
                    )
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(SubsNotSupportedException())
                }
            )
            ?.also { billingService = it }
            ?.also { callback?.connectionSucceed?.invoke() }
    }

    private fun isPurchaseTypeSupported(
        purchaseType: PurchaseType,
        inAppBillingService: IInAppBillingService
    ): Boolean {
        val supportState = inAppBillingService.isBillingSupported(
            Billing.IN_APP_BILLING_VERSION,
            context.packageName,
            purchaseType.type
        )
        return supportState == BazaarIntent.RESPONSE_RESULT_OK
    }

    fun purchase(
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {

        val intentSenderFire: (IntentSender) -> Unit = { intentSender ->
            activity.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        val intentFire: (Intent) -> Unit = { intent ->
            activity.startActivityForResult(
                intent,
                purchaseRequest.requestCode
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        purchase(purchaseRequest, purchaseType, callback, intentSenderFire, intentFire)
    }

    fun purchase(
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        val intentSenderFire: (IntentSender) -> Unit = { intentSender ->
            fragment.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0,
                null
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        val intentFire: (Intent) -> Unit = { intent ->
            fragment.startActivityForResult(
                intent,
                purchaseRequest.requestCode
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        purchase(purchaseRequest, purchaseType, callback, intentSenderFire, intentFire)
    }

    private fun purchase(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit,
        fireIntentSender: (IntentSender) -> Unit,
        fireIntent: (Intent) -> Unit
    ) = withService {
        purchaseFunction.function(
            billingService = this,
            request = PurchaseFunctionRequest(
                purchaseRequest,
                purchaseType,
                callback,
                fireIntentSender,
                fireIntent
            )
        )
    } ifServiceIsDisconnected {
        PurchaseIntentCallback().apply(callback).failedToBeginFlow.invoke(DisconnectException())
    }

    fun consume(
        purchaseToken: String,
        callback: ConsumeCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        consumeFunction.function(
            billingService = this,
            request = ConsumeFunctionRequest(purchaseToken, callback)
        )
    } ifServiceIsDisconnected {
        ConsumeCallback().apply(callback).consumeFailed.invoke(DisconnectException())
    }

    fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        queryFunction.function(
            billingService = this,
            request = QueryFunctionRequest(purchaseType, callback)
        )
    } ifServiceIsDisconnected {
        PurchaseQueryCallback().apply(callback).queryFailed.invoke(DisconnectException())
    }

    fun getSkuDetail(
        purchaseType: PurchaseType,
        skuIds: List<String>,
        callback: GetSkuDetailsCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        skuDetailFunction.function(
            billingService = this,
            request = SkuDetailFunctionRequest(purchaseType, skuIds, callback)
        )
    }

    private fun stopConnection() {
        if (billingService != null) {
            context.unbindService(this)
            disconnect()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        disconnect()
    }

    private fun disconnect() {
        billingService = null
        callback?.disconnected?.invoke()
        callback = null
        backgroundThread.dispose()
    }

    private inline fun withService(
        runOnBackground: Boolean = false,
        crossinline service: IInAppBillingService.() -> Unit
    ): ConnectionState {
        return billingService?.also {
            if (runOnBackground) {
                backgroundThread.execute(Runnable { service.invoke(it) })
            } else {
                service.invoke(it)
            }
        }?.let { ConnectionState.Connected }
            ?: run { ConnectionState.Disconnected }
    }

    private inline infix fun ConnectionState.ifServiceIsDisconnected(block: () -> Unit) {
        if (this is ConnectionState.Disconnected) {
            block.invoke()
        }
    }

    companion object {
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
    }
}
