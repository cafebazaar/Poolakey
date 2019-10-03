package com.phelat.poolakey

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService

internal class BillingConnection(private val context: Context) : ServiceConnection {

    private var callback: ConnectionCallback? = null

    private var billingService: IInAppBillingService? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback().apply(connectionCallback)
        Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf { context.packageManager.queryIntentServices(it, 0).isNotEmpty() }
            ?.also { context.bindService(it, this, Context.BIND_AUTO_CREATE) }
            ?: run { callback?.connectionFailed?.invoke() }
        return requireNotNull(callback)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        IInAppBillingService.Stub.asInterface(service)
            ?.takeIf { isInAppBillingSupported(it) }
            ?.also { billingService = it }
            ?.also { callback?.connectionSucceed?.invoke() }
            ?: run { callback?.connectionFailed?.invoke() }
    }

    private fun isInAppBillingSupported(inAppBillingService: IInAppBillingService): Boolean {
        val inAppBillingSupportState = inAppBillingService.isBillingSupported(
            IN_APP_BILLING_VERSION,
            context.packageName,
            PurchaseType.IN_APP.type
        )
        return inAppBillingSupportState == SERVICE_RESPONSE_RESULT_OK
    }

    fun purchase(activity: Activity, purchaseRequest: PurchaseRequest, purchaseType: PurchaseType) {
        billingService?.getBuyIntent(
            IN_APP_BILLING_VERSION,
            context.packageName,
            purchaseRequest.sku,
            purchaseType.type,
            purchaseRequest.extraData
        )?.takeIf { it.get(INTENT_RESPONSE_CODE) == SERVICE_RESPONSE_RESULT_OK }
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

    override fun onServiceDisconnected(name: ComponentName?) {
        billingService = null
        callback?.disconnected?.invoke()
    }

    companion object {
        private const val SERVICE_RESPONSE_RESULT_OK = 0
        private const val IN_APP_BILLING_VERSION = 3
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
        private const val INTENT_RESPONSE_CODE = "RESPONSE_CODE"
        private const val INTENT_RESPONSE_BUY = "BUY_INTENT"
    }

}
