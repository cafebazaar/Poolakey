package com.phelat.poolakey

import android.content.Context

class Payment(context: Context) {

    private val connection = BillingConnection(context)

    fun initialize(callback: ConnectionCallback.() -> Unit) {
        connection.startConnection(callback)
    }

}
