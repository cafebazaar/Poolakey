package ir.cafebazaar.poolakey.billing.purchase

import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback

internal data class PurchaseWeakHolder<T>(
    val component: T,
    val requestCode: Int,
    val callback: PurchaseIntentCallback.() -> Unit
)