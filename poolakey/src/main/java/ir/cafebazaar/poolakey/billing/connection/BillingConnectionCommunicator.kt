package ir.cafebazaar.poolakey.billing.connection

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.request.PurchaseRequest

internal interface BillingConnectionCommunicator {

    fun startConnection(
        context: Context,
        callback: ConnectionCallback
    ): Boolean

    fun consume(
        purchaseToken: String,
        callback: ConsumeCallback.() -> Unit
    )

    fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    )

    fun purchase(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit,
        fireIntentSender: (IntentSender) -> Unit,
        fireIntent: (Intent) -> Unit
    )

    fun stopConnection()

    fun disconnect()
}
