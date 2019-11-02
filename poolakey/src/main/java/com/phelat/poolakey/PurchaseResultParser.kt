package com.phelat.poolakey

import android.content.Intent
import com.phelat.poolakey.callback.PurchaseCallback
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.entity.PurchaseEntity
import com.phelat.poolakey.mapper.RawDataToPurchaseInfo

internal class PurchaseResultParser(private val rawDataToPurchaseInfo: RawDataToPurchaseInfo) {

    fun handleReceivedResult(data: Intent?, purchaseCallback: PurchaseCallback.() -> Unit) {
        if (data?.extras?.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK) {
            parseResult(data, purchaseCallback)
        } else {
            PurchaseCallback().apply(purchaseCallback)
                .purchaseFailed
                .invoke(IllegalStateException("Response code is not valid"))
        }
    }

    private fun parseResult(data: Intent?, purchaseCallback: PurchaseCallback.() -> Unit) {
        val purchaseData = data?.getStringExtra(BazaarIntent.RESPONSE_PURCHASE_DATA)
        val dataSignature = data?.getStringExtra(BazaarIntent.RESPONSE_SIGNATURE_DATA)
        if (purchaseData != null && dataSignature != null) {
            PurchaseEntity(
                purchaseInfo = rawDataToPurchaseInfo.mapToPurchaseInfo(purchaseData),
                dataSignature = dataSignature
            ).also { purchaseInfo ->
                PurchaseCallback().apply(purchaseCallback).purchaseSucceed.invoke(purchaseInfo)
            }
        } else {
            PurchaseCallback().apply(purchaseCallback)
                .purchaseFailed
                .invoke(IllegalStateException("Received data is not valid"))
        }
    }

}
