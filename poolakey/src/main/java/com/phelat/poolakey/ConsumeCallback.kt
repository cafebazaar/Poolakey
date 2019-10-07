package com.phelat.poolakey

class ConsumeCallback {

    internal var consumeSucceed: () -> Unit = {}

    internal var consumeFailed: () -> Unit = {}

    fun consumeSucceed(block: () -> Unit) {
        consumeSucceed = block
    }

    fun consumeFailed(block: () -> Unit) {
        consumeFailed = block
    }

}
