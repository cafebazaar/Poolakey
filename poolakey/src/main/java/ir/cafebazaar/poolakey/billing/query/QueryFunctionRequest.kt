package ir.cafebazaar.poolakey.billing.query

import android.os.Bundle
import ir.cafebazaar.poolakey.billing.FunctionRequest
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback

internal class QueryFunctionRequest(
    val purchaseType: String,
    val queryBundle: (purchaseType: String, continuation: String?) -> Bundle?,
    val callback: PurchaseQueryCallback.() -> Unit
) : FunctionRequest