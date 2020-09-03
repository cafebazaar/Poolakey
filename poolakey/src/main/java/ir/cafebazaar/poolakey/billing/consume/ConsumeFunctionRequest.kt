package ir.cafebazaar.poolakey.billing.consume

import ir.cafebazaar.poolakey.billing.FunctionRequest
import ir.cafebazaar.poolakey.callback.ConsumeCallback

internal class ConsumeFunctionRequest(
    val purchaseToken: String,
    val callback: ConsumeCallback.() -> Unit
): FunctionRequest