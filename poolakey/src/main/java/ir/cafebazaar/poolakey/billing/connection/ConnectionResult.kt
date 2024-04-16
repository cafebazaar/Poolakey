package ir.cafebazaar.poolakey.billing.connection

import java.lang.Exception

sealed class ConnectionResult {

    object Success : ConnectionResult()

    data class Failed(val exception: Exception) : ConnectionResult()
}