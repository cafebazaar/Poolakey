package ir.cafebazaar.poolakey

internal data class ConnectionRequestResult(
    val canConnect: Boolean,
    val canUseFallback: Boolean = true
)
