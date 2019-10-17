package com.phelat.poolakey.callback

class ConsumeCallback {

    internal var consumeSucceed: () -> Unit = {}

    internal var consumeFailed: (exception: Throwable) -> Unit = {}

    fun consumeSucceed(block: () -> Unit) {
        consumeSucceed = block
    }

    fun consumeFailed(block: (exception: Throwable) -> Unit) {
        consumeFailed = block
    }

}
