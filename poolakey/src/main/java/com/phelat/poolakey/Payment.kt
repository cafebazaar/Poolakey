package com.phelat.poolakey

import android.content.Context

class Payment(context: Context) {

    private val connection = BillingConnection(context)

    fun initialize(callback: ConnectionCallback.() -> Unit): ConnectionCallback {
        return connection.startConnection(callback)
    }

}
