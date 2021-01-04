package ir.cafebazaar.poolakey.billing.connection

import android.content.*
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService
import ir.cafebazaar.poolakey.ConnectionState
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.consume.ConsumeFunction
import ir.cafebazaar.poolakey.billing.consume.ConsumeFunctionRequest
import ir.cafebazaar.poolakey.billing.purchase.PurchaseFunction
import ir.cafebazaar.poolakey.billing.purchase.PurchaseFunctionRequest
import ir.cafebazaar.poolakey.billing.query.QueryFunction
import ir.cafebazaar.poolakey.billing.query.QueryFunctionRequest
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.constant.Billing
import ir.cafebazaar.poolakey.exception.BazaarNotFoundException
import ir.cafebazaar.poolakey.exception.DisconnectException
import ir.cafebazaar.poolakey.exception.IAPNotSupportedException
import ir.cafebazaar.poolakey.exception.SubsNotSupportedException
import ir.cafebazaar.poolakey.mapper.RawDataToPurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.security.PurchaseVerifier
import ir.cafebazaar.poolakey.security.Security
import ir.cafebazaar.poolakey.takeIf
import ir.cafebazaar.poolakey.thread.PoolakeyThread
import java.lang.ref.WeakReference

internal class ServiceBillingConnection(
    context: Context,
    mainThread: PoolakeyThread<() -> Unit>,
    private val backgroundThread: PoolakeyThread<Runnable>,
    private val paymentConfiguration: PaymentConfiguration
) : BillingConnectionCommunicator, ServiceConnection {

    private val purchaseFunction = PurchaseFunction(context)

    private val consumeFunction = ConsumeFunction(mainThread, context)

    private val queryFunction = QueryFunction(
        RawDataToPurchaseInfo(),
        PurchaseVerifier(),
        paymentConfiguration,
        mainThread,
        context
    )

    private var billingService: IInAppBillingService? = null
    private var callbackReference: WeakReference<ConnectionCallback>? = null
    private var contextReference: WeakReference<Context>? = null

    override fun startConnection(context: Context, callback: ConnectionCallback): Boolean {
        callbackReference = WeakReference(callback)
        contextReference = WeakReference(context)

        return Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf(
                thisIsTrue = {
                    context.packageManager.queryIntentServices(it, 0).isNullOrEmpty().not()
                },
                andIfNot = {
                    callback.connectionFailed.invoke(BazaarNotFoundException())
                }
            )?.takeIf(
                thisIsTrue = {
                    Security.verifyBazaarIsInstalled(context)
                },
                andIfNot = {
                    callback.connectionFailed.invoke(BazaarNotFoundException())
                }
            )?.let {
                try {
                    context.bindService(it, this, Context.BIND_AUTO_CREATE)
                } catch (e: SecurityException) {
                    callback.connectionFailed.invoke(e)
                    false
                }
            } ?: false
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        IInAppBillingService.Stub.asInterface(service)
            ?.also { billingService = it }
            ?.takeIf(
                thisIsTrue = {
                    isPurchaseTypeSupported(
                        purchaseType = PurchaseType.IN_APP
                    )
                },
                andIfNot = {
                    callbackReference?.get()?.connectionFailed?.invoke(IAPNotSupportedException())
                }
            )
            ?.takeIf(
                thisIsTrue = {
                    !paymentConfiguration.shouldSupportSubscription || isPurchaseTypeSupported(
                        purchaseType = PurchaseType.SUBSCRIPTION
                    )
                },
                andIfNot = {
                    callbackReference?.get()?.connectionFailed?.invoke(SubsNotSupportedException())
                }
            )
            ?.also { callbackReference?.get()?.connectionSucceed?.invoke() }
    }

    override fun isPurchaseTypeSupported(
        purchaseType: PurchaseType
    ): Boolean {
        return contextReference?.get()?.let { context ->
            val supportState = billingService?.isBillingSupported(
                Billing.IN_APP_BILLING_VERSION,
                context.packageName,
                purchaseType.type
            )

            supportState == BazaarIntent.RESPONSE_RESULT_OK
        } ?: false
    }

    override fun consume(
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

    override fun queryPurchasedProducts(
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

    override fun purchase(
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

    override fun stopConnection() {
        if (billingService != null) {
            contextReference?.get()?.unbindService(this)
            disconnect()
        }
    }

    private inline infix fun ConnectionState.ifServiceIsDisconnected(block: () -> Unit) {
        if (this is ConnectionState.Disconnected) {
            block.invoke()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        disconnect()
    }

    override fun disconnect() {
        billingService = null
    }

    companion object {
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
    }
}