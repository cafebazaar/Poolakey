package ir.cafebazaar.poolakey.mapper

import ir.cafebazaar.poolakey.constant.RawJson
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.PurchaseState
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

internal class RawDataToPurchaseInfo {

    fun mapToPurchaseInfo(purchaseData: String, dataSignature: String): PurchaseInfo {
        return JSONObject(purchaseData).run {
            PurchaseInfo(
                orderId = optString(RawJson.ORDER_ID),
                purchaseToken = optString(RawJson.PURCHASE_TOKEN),
                payload = optString(RawJson.DEVELOPER_PAYLOAD),
                packageName = optString(RawJson.PACKAGE_NAME),
                purchaseState = if (optInt(RawJson.PURCHASE_STATE) == 0) PurchaseState.PURCHASED else PurchaseState.REFUNDED,
                purchaseTime = optLong(RawJson.PURCHASE_TIME),
                productId = optString(RawJson.PRODUCT_ID),
                dataSignature = dataSignature,
                originalJson = purchaseData
            )
        }
    }

    fun mapList(purchases: List<Pair<String, String>>): List<PurchaseInfo> {
        return purchases.map { (data, signature) -> mapToPurchaseInfo(data, signature) }
    }

    fun getPurchaseSummary(purchaseData: String): String {
        val json = JSONObject(purchaseData)
        val state = if (json.optInt(RawJson.PURCHASE_STATE) == 0) "✅ PURCHASED" else "❌ REFUNDED"
        val formattedTime = formatPurchaseTime(purchaseData)
        return buildString {
            appendLine("━━━━━━━━━ Purchase Summary ━━━━━━━━━")
            appendLine("Order ID     : ${json.optString(RawJson.ORDER_ID)}")
            appendLine("Product ID   : ${json.optString(RawJson.PRODUCT_ID)}")
            appendLine("Purchase State: $state")
            appendLine("Purchase Time: $formattedTime")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    fun isRefunded(purchaseData: String): Boolean {
        return JSONObject(purchaseData).optInt(RawJson.PURCHASE_STATE) != 0
    }

    fun getField(purchaseData: String, field: String): String? {
        val json = JSONObject(purchaseData)
        return json.optString(field, null)
    }

    /**
     * Placeholder for verifying purchase signature.
     */
    fun verifyPurchaseSignature(purchaseData: String, signature: String, publicKey: String): Boolean {
        if (signature.isBlank() || purchaseData.isBlank()) {
            println("⚠️ Signature verification placeholder - not implemented!")
            return false
        }
        return true
    }

    fun filterByState(purchases: List<PurchaseInfo>, state: PurchaseState): List<PurchaseInfo> {
        return purchases.filter { it.purchaseState == state }
    }

    fun getMostRecentPurchase(purchases: List<PurchaseInfo>): PurchaseInfo? {
        return purchases.maxByOrNull { it.purchaseTime }
    }

    fun formatPurchaseTime(purchaseData: String): String {
        val time = JSONObject(purchaseData).optLong(RawJson.PURCHASE_TIME)
        return if (time > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))
        } else {
            "N/A"
        }
    }

    fun isPurchaseForProduct(purchaseData: String, productId: String): Boolean {
        return JSONObject(purchaseData).optString(RawJson.PRODUCT_ID) == productId
    }
}
