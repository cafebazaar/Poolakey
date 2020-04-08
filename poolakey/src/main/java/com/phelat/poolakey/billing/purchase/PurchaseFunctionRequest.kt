package com.phelat.poolakey.billing.purchase

import android.content.Intent
import android.content.IntentSender
import com.phelat.poolakey.PurchaseType
import com.phelat.poolakey.billing.FunctionRequest
import com.phelat.poolakey.callback.PurchaseIntentCallback
import com.phelat.poolakey.request.PurchaseRequest

internal class PurchaseFunctionRequest(
    val purchaseRequest: PurchaseRequest,
    val purchaseType: PurchaseType,
    val callback: PurchaseIntentCallback.() -> Unit,
    val fireIntentWithIntentSender: (IntentSender) -> Unit,
    val fireIntentWithIntent: (Intent) -> Unit
) : FunctionRequest