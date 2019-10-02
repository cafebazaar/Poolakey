package com.phelat.poolakey

sealed class ConnectionState {

    object Connected : ConnectionState()

    object FailedToConnect : ConnectionState()

    object Disconnected : ConnectionState()

}
