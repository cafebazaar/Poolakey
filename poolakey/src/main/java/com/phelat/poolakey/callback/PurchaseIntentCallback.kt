package com.phelat.poolakey.callback

class PurchaseIntentCallback {

    internal var purchaseFlowBegan: () -> Unit = {}

    internal var failedToBeginFlow: (throwable: Throwable) -> Unit = {}

    fun purchaseFlowBegan(block: () -> Unit) {
        purchaseFlowBegan = block
    }

    fun failedToBeginFlow(block: (throwable: Throwable) -> Unit) {
        failedToBeginFlow = block
    }

}
