package ir.cafebazaar.poolakey.request

import android.os.Bundle
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.constant.BazaarIntent

data class PurchaseRequest(
    val productId: String,
    val payload: String? = null,
    val dynamicPriceToken: String? = null
)

internal fun purchaseExtraData(
    purchaseRequest: PurchaseRequest,
    purchaseType: PurchaseType
): Bundle {
    return Bundle().apply {
        putString(BazaarIntent.RESPONSE_DYNAMIC_PRICE_TOKEN, purchaseRequest.dynamicPriceToken)
        putString(BazaarIntent.RESPONSE_PURCHASE_TYPE, purchaseType.type)
    }
}