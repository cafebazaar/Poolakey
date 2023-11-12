package ir.cafebazaar.poolakey.config

data class PaymentConfiguration @JvmOverloads constructor(
    val localSecurityCheck: SecurityCheck,
    val shouldSupportSubscription: Boolean = true
)