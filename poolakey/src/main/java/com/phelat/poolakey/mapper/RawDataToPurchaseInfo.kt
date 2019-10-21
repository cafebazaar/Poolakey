package com.phelat.poolakey.mapper

import com.phelat.poolakey.entity.PurchaseInfo
import org.json.JSONObject

internal class RawDataToPurchaseInfo {

    fun mapToPurchaseInfo(purchaseData: String): PurchaseInfo {
        return JSONObject(purchaseData).run {
            PurchaseInfo(
                orderId = optString(ORDER_ID),
                purchaseToken = optString(PURCHASE_TOKEN),
                payload = optString(DEVELOPER_PAYLOAD),
                packageName = optString(PACKAGE_NAME),
                purchaseState = optInt(PURCHASE_STATE),
                purchaseTime = optLong(PURCHASE_TIME),
                productId = optString(PRODUCT_ID)
            )
        }
    }

    companion object {
        private const val ORDER_ID: String = "orderId"
        private const val PURCHASE_TOKEN: String = "purchaseToken"
        private const val DEVELOPER_PAYLOAD: String = "developerPayload"
        private const val PACKAGE_NAME: String = "packageName"
        private const val PURCHASE_STATE: String = "purchaseState"
        private const val PURCHASE_TIME: String = "purchaseTime"
        private const val PRODUCT_ID: String = "productId"
    }

}
