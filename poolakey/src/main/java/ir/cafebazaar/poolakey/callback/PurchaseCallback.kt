package ir.cafebazaar.poolakey.callback

import ir.cafebazaar.poolakey.entity.PurchaseInfo

class PurchaseCallback {

    internal var purchaseSucceed: (PurchaseInfo) -> Unit = {}

    internal var purchaseCanceled: () -> Unit = {}

    internal var purchaseFailed: (throwable: Throwable) -> Unit = {}

    internal var purchaseFlowBegan: () -> Unit = {}

    internal var failedToBeginFlow: (throwable: Throwable) -> Unit = {}

    fun purchaseSucceed(block: (PurchaseInfo) -> Unit) {
        purchaseSucceed = block
    }

    fun purchaseCanceled(block: () -> Unit) {
        purchaseCanceled = block
    }

    fun purchaseFailed(block: (throwable: Throwable) -> Unit) {
        purchaseFailed = block
    }

    fun purchaseFlowBegan(block: () -> Unit) {
        purchaseFlowBegan = block
    }

    fun failedToBeginFlow(block: (throwable: Throwable) -> Unit) {
        failedToBeginFlow = block
    }
}
