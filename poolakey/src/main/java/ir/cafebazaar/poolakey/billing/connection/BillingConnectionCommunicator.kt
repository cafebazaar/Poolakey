package ir.cafebazaar.poolakey.billing.connection

import android.content.Context
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.PaymentLauncher
import ir.cafebazaar.poolakey.billing.skudetail.SkuDetailFunctionRequest
import ir.cafebazaar.poolakey.billing.trialsubscription.CheckTrialSubscriptionFunctionRequest
import ir.cafebazaar.poolakey.callback.CheckTrialSubscriptionCallback
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.callback.PurchaseCallback
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
        paymentLauncher: PaymentLauncher,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseCallback.() -> Unit
    )

    fun getSkuDetails(
        request: SkuDetailFunctionRequest,
        callback: GetSkuDetailsCallback.() -> Unit,
    )

    fun checkTrialSubscription(
        request: CheckTrialSubscriptionFunctionRequest,
        callback: CheckTrialSubscriptionCallback.() -> Unit,
    )

    fun stopConnection()
}
