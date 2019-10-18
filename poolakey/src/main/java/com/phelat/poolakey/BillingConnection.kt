package com.phelat.poolakey

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService
import com.phelat.poolakey.callback.ConnectionCallback
import com.phelat.poolakey.callback.ConsumeCallback
import com.phelat.poolakey.callback.PurchaseIntentCallback
import com.phelat.poolakey.config.PaymentConfiguration
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.exception.BazaarNotFoundException
import com.phelat.poolakey.exception.ConsumeFailedException
import com.phelat.poolakey.exception.DisconnectException
import com.phelat.poolakey.exception.IAPNotSupportedException
import com.phelat.poolakey.exception.SubsNotSupportedException
import com.phelat.poolakey.request.PurchaseRequest

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration
) : ServiceConnection {

    private var callback: ConnectionCallback? = null

    private var billingService: IInAppBillingService? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)
        Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf(
                thisIsTrue = {
                    context.packageManager.queryIntentServices(it, 0).isNotEmpty()
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(BazaarNotFoundException())
                }
            )
            ?.also { context.bindService(it, this, Context.BIND_AUTO_CREATE) }
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
            IN_APP_BILLING_VERSION,
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
    ) = withService {
        getBuyIntent(
            IN_APP_BILLING_VERSION,
            context.packageName,
            purchaseRequest.productId,
            purchaseType.type,
            purchaseRequest.payload
        )?.takeIf { it.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK }
            ?.getParcelable<PendingIntent>(INTENT_RESPONSE_BUY)
            ?.also { purchaseIntent ->
                activity.startIntentSenderForResult(
                    purchaseIntent.intentSender,
                    purchaseRequest.requestCode,
                    Intent(),
                    0,
                    0,
                    0
                )
            }
    } ifServiceIsDisconnected {
        TODO("Handle disconnect state")
    }

    fun consume(purchaseToken: String, callback: ConsumeCallback.() -> Unit) = withService {
        consumePurchase(IN_APP_BILLING_VERSION, context.packageName, purchaseToken)
            .takeIf(
                thisIsTrue = { it == BazaarIntent.RESPONSE_RESULT_OK },
                andIfNot = {
                    ConsumeCallback().apply(callback)
                        .consumeFailed
                        .invoke(ConsumeFailedException())
                }
            )
            ?.also { ConsumeCallback().apply(callback).consumeSucceed.invoke() }
    } ifServiceIsDisconnected {
        ConsumeCallback().apply(callback).consumeFailed.invoke(DisconnectException())
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
    }

    private inline fun withService(service: IInAppBillingService.() -> Unit): ConnectionState {
        return billingService?.also { service.invoke(it) }
            ?.let { ConnectionState.Connected }
            ?: run { ConnectionState.Disconnected }
    }

    private inline infix fun ConnectionState.ifServiceIsDisconnected(block: () -> Unit) {
        if (this is ConnectionState.Disconnected) {
            block.invoke()
        }
    }

    companion object {
        private const val IN_APP_BILLING_VERSION = 3
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
        private const val INTENT_RESPONSE_BUY = "BUY_INTENT"
    }

}
