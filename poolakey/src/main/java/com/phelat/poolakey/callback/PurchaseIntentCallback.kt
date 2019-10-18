package com.phelat.poolakey.callback

class PurchaseIntentCallback {

    internal var purchaseFlowBegan: () -> Unit = {}

    internal var failedToBeginFlow: () -> Unit = {}

    fun purchaseFlowBegan(block: () -> Unit) {
        purchaseFlowBegan = block
    }

    fun failedToBeginFlow(block: () -> Unit) {
        failedToBeginFlow = block
    }

}
