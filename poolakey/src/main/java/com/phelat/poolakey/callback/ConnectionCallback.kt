package com.phelat.poolakey.callback

import com.phelat.poolakey.Connection
import com.phelat.poolakey.ConnectionState

class ConnectionCallback(private val disconnect: () -> Unit) : Connection {

    private var connectionState: ConnectionState = ConnectionState.Disconnected

    internal var connectionSucceed: () -> Unit = {}

    internal var connectionFailed: () -> Unit = {}

    internal var disconnected: () -> Unit = {}

    fun connectionSucceed(block: () -> Unit) {
        connectionSucceed = {
            connectionState = ConnectionState.Connected
            block.invoke()
        }
    }

    fun connectionFailed(block: () -> Unit) {
        connectionFailed = {
            connectionState = ConnectionState.FailedToConnect
            block.invoke()
        }
    }

    fun disconnected(block: () -> Unit) {
        disconnected = {
            connectionState = ConnectionState.Disconnected
            block.invoke()
        }
    }

    override fun getState(): ConnectionState {
        return connectionState
    }

    override fun disconnect() {
        disconnect.invoke()
    }

}
