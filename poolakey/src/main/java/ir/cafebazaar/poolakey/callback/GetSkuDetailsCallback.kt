package ir.cafebazaar.poolakey.callback

import ir.cafebazaar.poolakey.entity.SkuDetails

class GetSkuDetailsCallback {

    internal var getSkuDetailsSucceed: (List<SkuDetails>) -> Unit = {}

    internal var getSkuDetailsFailed: (throwable: Throwable) -> Unit = {}

    fun getSkuDetailsSucceed(block: (List<SkuDetails>) -> Unit) {
        getSkuDetailsSucceed = block
    }

    fun getSkuDetailsFailed(block: (throwable: Throwable) -> Unit) {
        getSkuDetailsFailed = block
    }

}