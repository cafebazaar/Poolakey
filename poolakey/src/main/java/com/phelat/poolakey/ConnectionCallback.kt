package com.phelat.poolakey

class ConnectionCallback {

    lateinit var connectionState: ConnectionState

    internal var connectionSucceed: () -> Unit = {}

    internal var connectionFailed: () -> Unit = {}

    internal var disconnected: () -> Unit = {}

    fun connectionSucceed(block: () -> Unit) {
        connectionState = ConnectionState.Connected
        connectionSucceed = block
    }

    fun connectionFailed(block: () -> Unit) {
        connectionState = ConnectionState.FailedToConnect
        connectionFailed = block
    }

    fun disconnected(block: () -> Unit) {
        connectionState = ConnectionState.Disconnected
        disconnected = block
    }

}
