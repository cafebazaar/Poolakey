package com.phelat.poolakey

class ConnectionCallback(private val disconnect: () -> Unit) : Connection {

    private var connectionState: ConnectionState = ConnectionState.Disconnected

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

    override fun getState(): ConnectionState {
        return connectionState
    }

    override fun disconnect() {
        disconnect.invoke()
    }

}
