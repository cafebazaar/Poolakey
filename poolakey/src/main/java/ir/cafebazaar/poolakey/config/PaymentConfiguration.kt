package ir.cafebazaar.poolakey.config

data class PaymentConfiguration(
    val localSecurityCheck: SecurityCheck,
    val shouldSupportSubscription: Boolean = true
)
