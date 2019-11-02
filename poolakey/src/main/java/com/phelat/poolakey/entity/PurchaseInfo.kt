package com.phelat.poolakey.entity

import com.phelat.poolakey.constant.RawJson
import org.json.JSONObject

data class PurchaseInfo(
    val orderId: String,
    val purchaseToken: String,
    val payload: String,
    val packageName: String,
    val purchaseState: PurchaseState,
    val purchaseTime: Long,
    val productId: String
) {

    fun toJson() = with(JSONObject()) {
        put(RawJson.ORDER_ID, orderId)
        put(RawJson.PURCHASE_TOKEN, purchaseToken)
        put(RawJson.DEVELOPER_PAYLOAD, payload)
        put(RawJson.PACKAGE_NAME, packageName)
        put(RawJson.PURCHASE_STATE, purchaseState.ordinal)
        put(RawJson.PURCHASE_TIME, purchaseTime)
        put(RawJson.PRODUCT_ID, productId)
    }

}
