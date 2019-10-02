package com.phelat.poolakey

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService

internal class BillingConnection(private val context: Context) : ServiceConnection {

    private var callback: ConnectionCallback? = null

    private var billingService: IInAppBillingService? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit) {
        callback = ConnectionCallback().apply(connectionCallback)
        Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf { context.packageManager.queryIntentServices(it, 0).isNotEmpty() }
            ?.also { context.bindService(it, this, Context.BIND_AUTO_CREATE) }
            ?: run { callback?.connectionFailed?.invoke() }
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
            BILLING_ITEM_TYPE_IN_APP
        )
        return inAppBillingSupportState == SERVICE_RESPONSE_RESULT_OK
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        billingService = null
    }

    companion object {
        private const val SERVICE_RESPONSE_RESULT_OK = 0
        private const val IN_APP_BILLING_VERSION = 3
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
        private const val BILLING_ITEM_TYPE_IN_APP = "inapp"
    }

}
