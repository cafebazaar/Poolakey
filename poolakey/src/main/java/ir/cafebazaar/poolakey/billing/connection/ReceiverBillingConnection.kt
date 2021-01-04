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

internal class ReceiverBillingConnection: BillingConnectionCommunicator {

    override fun startConnection(context: Context, callback: ConnectionCallback): Boolean {
        TODO("Not yet implemented")
    }

    override fun isPurchaseTypeSupported(purchaseType: PurchaseType): Boolean {
        TODO("Not yet implemented")
    }

    override fun consume(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        TODO("Not yet implemented")
    }

    override fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun purchase(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit,
        fireIntentSender: (IntentSender) -> Unit,
        fireIntent: (Intent) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun stopConnection() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }
}