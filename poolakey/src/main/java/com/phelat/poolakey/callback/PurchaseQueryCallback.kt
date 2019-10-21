package com.phelat.poolakey.callback

import com.phelat.poolakey.entity.PurchaseInfo

class PurchaseQueryCallback {

    internal var querySucceed: (List<PurchaseInfo>) -> Unit = {}

    internal var queryFailed: () -> Unit = {}

    fun querySucceed(block: (List<PurchaseInfo>) -> Unit) {
        querySucceed = block
    }

    fun queryFailed(block: () -> Unit) {
        queryFailed = block
    }

}
