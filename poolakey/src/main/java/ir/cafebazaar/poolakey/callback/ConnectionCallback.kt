package ir.cafebazaar.poolakey.callback

import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.ConnectionState

class ConnectionCallback(private val disconnect: () -> Unit) : Connection {

    private var connectionState: ConnectionState = ConnectionState.Disconnected

    internal var connectionSucceed: () -> Unit = {}

    internal var connectionFailed: (throwable: Throwable) -> Unit = {}

    internal var disconnected: () -> Unit = {}

    fun connectionSucceed(block: () -> Unit) {
        connectionSucceed = {
            connectionState = ConnectionState.Connected
            block.invoke()
        }
    }

    fun connectionFailed(block: (throwable: Throwable) -> Unit) {
        connectionFailed = {
            connectionState = ConnectionState.FailedToConnect
            block.invoke(it)
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
