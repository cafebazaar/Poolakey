package com.phelat.poolakey.callback

import com.phelat.poolakey.entity.PurchaseEntity

class PurchaseCallback {

    internal var purchaseSucceed: (PurchaseEntity) -> Unit = {}

    internal var purchaseCanceled: () -> Unit = {}

    internal var purchaseFailed: (throwable: Throwable) -> Unit = {}

    fun purchaseSucceed(block: (PurchaseEntity) -> Unit) {
        purchaseSucceed = block
    }

    fun purchaseCanceled(block: () -> Unit) {
        purchaseCanceled = block
    }

    fun purchaseFailed(block: (throwable: Throwable) -> Unit) {
        purchaseFailed = block
    }

}
