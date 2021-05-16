package ir.cafebazaar.poolakey.billing.skudetail

import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.FunctionRequest
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback

internal class SkuDetailFunctionRequest(
    val purchaseType: PurchaseType,
    val skuIds: List<String>,
    val callback: GetSkuDetailsCallback.() -> Unit
) : FunctionRequest