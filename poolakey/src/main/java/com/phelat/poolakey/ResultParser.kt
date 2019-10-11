package com.phelat.poolakey

import android.content.Intent
import com.phelat.poolakey.callback.PurchaseCallback
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.entity.PurchaseInfo
import org.json.JSONObject

internal class ResultParser {

    private var callback: PurchaseCallback? = null

    fun parseResult(data: Intent?, purchaseCallback: PurchaseCallback.() -> Unit) {
        callback = PurchaseCallback().apply(purchaseCallback)
        if (data?.extras?.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK) {
            val purchaseData = data.getStringExtra(BazaarIntent.RESPONSE_PURCHASE_DATA)
            val dataSignature = data.getStringExtra(BazaarIntent.RESPONSE_SIGNATURE_DATA)
            if (purchaseData != null && dataSignature != null) {
                JSONObject(purchaseData).run {
                    PurchaseInfo(
                        orderId = optString(ORDER_ID),
                        purchaseToken = optString(PURCHASE_TOKEN),
                        payload = optString(DEVELOPER_PAYLOAD),
                        packageName = optString(PACKAGE_NAME),
                        purchaseState = optInt(PURCHASE_STATE),
                        purchaseTime = optLong(PURCHASE_TIME),
                        productId = optString(PRODUCT_ID)
                    )
                }.also { purchaseInfo ->
                    callback?.purchaseSucceed?.invoke(purchaseInfo)
                }
            } else {
                callback?.purchaseFailed?.invoke()
            }
        } else {
            callback?.purchaseFailed?.invoke()
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
