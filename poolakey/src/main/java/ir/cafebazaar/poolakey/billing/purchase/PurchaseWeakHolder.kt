package ir.cafebazaar.poolakey.billing.purchase

import ir.cafebazaar.poolakey.PaymentLauncher
import ir.cafebazaar.poolakey.callback.PurchaseCallback

internal data class PurchaseWeakHolder(
    val paymentLauncher: PaymentLauncher,
    val callback: PurchaseCallback.() -> Unit
)