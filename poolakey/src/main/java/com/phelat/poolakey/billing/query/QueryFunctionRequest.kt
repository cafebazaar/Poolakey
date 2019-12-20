package com.phelat.poolakey.billing.query

import com.phelat.poolakey.PurchaseType
import com.phelat.poolakey.billing.FunctionRequest
import com.phelat.poolakey.callback.PurchaseQueryCallback

internal class QueryFunctionRequest(
    val purchaseType: PurchaseType,
    val callback: PurchaseQueryCallback.() -> Unit
) : FunctionRequest