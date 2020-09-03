package ir.cafebazaar.poolakey.billing.query

import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.FunctionRequest
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback

internal class QueryFunctionRequest(
    val purchaseType: PurchaseType,
    val callback: PurchaseQueryCallback.() -> Unit
) : FunctionRequest