package ir.cafebazaar.poolakey.request

import android.os.Bundle
import ir.cafebazaar.poolakey.constant.BazaarIntent

data class PurchaseRequest(
    val productId: String,
    val requestCode: Int,
    val payload: String? = null,
    val dynamicPriceToken: String?
)

internal fun PurchaseRequest.purchaseExtraData(): Bundle {
    return Bundle().apply {
        putString(BazaarIntent.RESPONSE_DYNAMIC_PRICE_TOKEN, dynamicPriceToken)
    }
}