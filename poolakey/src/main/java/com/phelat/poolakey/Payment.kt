package com.phelat.poolakey

import android.content.Context

class Payment(context: Context) {

    private val connection = BillingConnection(context)

    fun initialize(callback: ConnectionCallback.() -> Unit): Connection {
        return connection.startConnection(callback)
    }

}
