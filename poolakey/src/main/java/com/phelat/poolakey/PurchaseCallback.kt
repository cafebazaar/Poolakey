package com.phelat.poolakey

class PurchaseCallback {

    internal var purchaseSucceed: (PurchaseInfo) -> Unit = {}

    internal var purchaseCanceled: () -> Unit = {}

    internal var purchaseFailed: () -> Unit = {}

    fun purchaseSucceed(block: (PurchaseInfo) -> Unit) {
        purchaseSucceed = block
    }

    fun purchaseCanceled(block: () -> Unit) {
        purchaseCanceled = block
    }

    fun purchaseFailed(block: () -> Unit) {
        purchaseFailed = block
    }

}
