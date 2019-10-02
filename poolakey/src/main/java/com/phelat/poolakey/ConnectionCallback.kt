package com.phelat.poolakey

class ConnectionCallback {

    internal var connectionSucceed: () -> Unit = {}

    internal var connectionFailed: () -> Unit = {}

    fun connectionSucceed(block: () -> Unit) {
        connectionSucceed = block
    }

    fun connectionFailed(block: () -> Unit) {
        connectionFailed = block
    }

}
