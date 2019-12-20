package com.phelat.poolakey.billing.consume

import com.phelat.poolakey.billing.FunctionRequest
import com.phelat.poolakey.callback.ConsumeCallback

internal class ConsumeFunctionRequest(
    val purchaseToken: String,
    val callback: ConsumeCallback.() -> Unit
): FunctionRequest