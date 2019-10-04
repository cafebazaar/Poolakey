package com.phelat.poolakey

import android.app.Activity
import android.content.Context

class Payment(context: Context) {

    private val connection = BillingConnection(context)

    fun connect(callback: ConnectionCallback.() -> Unit = {}): Connection {
        return connection.startConnection(callback)
    }

    fun purchaseItem(activity: Activity, request: PurchaseRequest) {
        requestCode = request.requestCode
        connection.purchase(activity, request, PurchaseType.IN_APP)
    }

    companion object {
        @Volatile
        private var requestCode: Int = -1
    }

}
