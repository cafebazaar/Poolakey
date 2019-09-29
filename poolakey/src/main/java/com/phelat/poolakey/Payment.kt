package com.phelat.poolakey

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService

class Payment(private val context: Context) : ServiceConnection {

    private var billingService: IInAppBillingService? = null

    fun initialize() {
        Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf { context.packageManager.queryIntentServices(it, 0).isNotEmpty() }
            ?.also { context.bindService(it, this, Context.BIND_AUTO_CREATE) }
            ?: run {
                TODO("Service is not available")
            }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        billingService = IInAppBillingService.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        billingService = null
    }

    companion object {
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
    }

}