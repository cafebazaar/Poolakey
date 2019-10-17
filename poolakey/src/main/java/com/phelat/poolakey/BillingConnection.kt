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
import com.phelat.poolakey.config.PaymentConfiguration
import com.phelat.poolakey.constant.BazaarIntent
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
            .takeIf { context.packageManager.queryIntentServices(it, 0).isNotEmpty() }
            ?.also { context.bindService(it, this, Context.BIND_AUTO_CREATE) }
            ?: run { callback?.connectionFailed?.invoke() }
        return requireNotNull(callback)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        IInAppBillingService.Stub.asInterface(service)
            ?.takeIf {
                isPurchaseTypeSupported(
                    purchaseType = PurchaseType.IN_APP,
                    inAppBillingService = it
                )
            }
            ?.takeIf {
                !paymentConfiguration.shouldSupportSubscription || isPurchaseTypeSupported(
                    purchaseType = PurchaseType.SUBSCRIPTION,
                    inAppBillingService = it
                )
            }
            ?.also { billingService = it }
            ?.also { callback?.connectionSucceed?.invoke() }
            ?: run { callback?.connectionFailed?.invoke() }
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

    fun purchase(activity: Activity, purchaseRequest: PurchaseRequest, purchaseType: PurchaseType) {
        billingService?.getBuyIntent(
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
    }

    fun consume(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        billingService?.consumePurchase(IN_APP_BILLING_VERSION, context.packageName, purchaseToken)
            ?.takeIf { it == BazaarIntent.RESPONSE_RESULT_OK }
            ?.also { ConsumeCallback().apply(callback).consumeSucceed.invoke() }
            ?: run { ConsumeCallback().apply(callback).consumeFailed.invoke() }
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

    companion object {
        private const val IN_APP_BILLING_VERSION = 3
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
        private const val INTENT_RESPONSE_BUY = "BUY_INTENT"
    }

}
