package ir.cafebazaar.poolakey.billing.connection

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.skudetail.SkuDetailFunctionRequest
import ir.cafebazaar.poolakey.billing.trialsubscription.CheckTrialSubscriptionFunctionRequest
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.callback.CheckTrialSubscriptionCallback
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
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    )

    fun purchase(
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
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
