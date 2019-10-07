package com.phelat.poolakey

import android.app.Activity
import android.content.Context
import android.content.Intent

class Payment(context: Context) {

    private val connection = BillingConnection(context)

    private var resultParser = ResultParser()

    fun connect(callback: ConnectionCallback.() -> Unit = {}): Connection {
        return connection.startConnection(callback)
    }

    fun purchaseItem(activity: Activity, request: PurchaseRequest) {
        requestCode = request.requestCode
        connection.purchase(activity, request, PurchaseType.IN_APP)
    }

    fun consumeItem(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        connection.consume(purchaseToken, callback)
    }

    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        if (Payment.requestCode > -1 && Payment.requestCode == requestCode) {
            handleReceivedResult(resultCode, data, purchaseCallback)
        }
    }

    private fun handleReceivedResult(
        resultCode: Int,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        when (resultCode) {
            Activity.RESULT_OK -> resultParser.parseResult(data, purchaseCallback)
            Activity.RESULT_CANCELED -> PurchaseCallback().apply(purchaseCallback).purchaseCanceled.invoke()
            else -> PurchaseCallback().apply(purchaseCallback).purchaseFailed.invoke()
        }
    }

    companion object {
        @Volatile
        private var requestCode: Int = -1
    }

}
