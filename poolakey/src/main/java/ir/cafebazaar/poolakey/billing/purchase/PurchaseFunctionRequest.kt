package ir.cafebazaar.poolakey.billing.purchase

import android.content.Intent
import android.content.IntentSender
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.FunctionRequest
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.request.PurchaseRequest

internal class PurchaseFunctionRequest(
    val purchaseRequest: PurchaseRequest,
    val purchaseType: PurchaseType,
    val callback: PurchaseIntentCallback.() -> Unit,
    val launchIntentWithIntentSender: (IntentSender) -> Unit,
    val launchIntent: (Intent) -> Unit
) : FunctionRequest