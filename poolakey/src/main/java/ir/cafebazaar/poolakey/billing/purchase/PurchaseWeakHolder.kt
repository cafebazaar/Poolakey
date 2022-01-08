package ir.cafebazaar.poolakey.billing.purchase

import ir.cafebazaar.poolakey.ResultLauncher
import ir.cafebazaar.poolakey.callback.PurchaseCallback

internal data class PurchaseWeakHolder(
    val resultLauncher: ResultLauncher,
    val callback: PurchaseCallback.() -> Unit
)