package ir.cafebazaar.poolakey.request

import android.os.Bundle
import android.util.Base64
import ir.cafebazaar.poolakey.constant.BazaarIntent
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import org.json.JSONObject

data class PurchaseRequest(
    val productId: String,
    val payload: String? = null,
    val dynamicPriceToken: String? = null
) {

    internal var cutoutModeIsShortEdges = false

    // ===============================
    // ✅ VALIDATION & UTILITY
    // ===============================

    /** Checks that the request has a valid product ID. */
    fun isValid(): Boolean = productId.isNotBlank()

    /** Generates a unique payload if none exists. */
    fun generatePayloadIfEmpty(): PurchaseRequest =
        if (payload.isNullOrEmpty()) copy(payload = UUID.randomUUID().toString()) else this

    /** Returns the payload encoded as Base64 safely. */
    fun payloadBase64(): String? =
        payload?.toByteArray(StandardCharsets.UTF_8)?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    /** Enable or disable cutout mode (short edges) for UI. */
    fun enableCutoutModeShortEdges(enable: Boolean = true): PurchaseRequest {
        cutoutModeIsShortEdges = enable
        return this
    }

    /** Converts the request into a Bundle suitable for Bazaar SDK. */
    internal fun purchaseExtraData(): Bundle =
        Bundle().apply {
            putString(BazaarIntent.RESPONSE_DYNAMIC_PRICE_TOKEN, dynamicPriceToken)
            putBoolean(BazaarIntent.RESPONSE_CUTOUT_MODE_IS_SHORT_EDGES, cutoutModeIsShortEdges)
            putString(BazaarIntent.RESPONSE_PRODUCT_ID, productId)
            payload?.let { putString(BazaarIntent.RESPONSE_PAYLOAD, it) }
        }

    /** Human-readable debug summary with improved formatting. */
    fun debugSummary(): String =
        buildString {
            appendLine("🛒 PurchaseRequest Summary:")
            appendLine("───────────────────────────────")
            appendLine("Product ID          : $productId")
            appendLine("Payload             : ${payload ?: "N/A"}")
            appendLine("Dynamic Price Token : ${dynamicPriceToken ?: "N/A"}")
            appendLine("Cutout Mode Short Edges : $cutoutModeIsShortEdges")
            appendLine("───────────────────────────────")
        }

    // ===============================
    // 🔐 SECURITY & INTEGRITY
    // ===============================

    /** SHA-256 hash of the payload (empty string if null). */
    fun payloadSHA256(): String {
        val data = payload ?: ""
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks whether the dynamic price token is still valid.
     * Default expiry: 15 minutes (900,000 ms)
     */
    fun isDynamicPriceTokenValid(expiryMillis: Long = 15 * 60 * 1000): Boolean {
        return try {
            val decoded = dynamicPriceToken?.let { String(Base64.decode(it, Base64.DEFAULT)) } ?: return false
            val timestamp = decoded.toLongOrNull() ?: return false
            System.currentTimeMillis() - timestamp < expiryMillis
        } catch (_: Exception) {
            false
        }
    }

    // ===============================
    // 🌐 SERIALIZATION
    // ===============================

    /** Converts the request to JSON for logging/network use. */
    fun toJson(): JSONObject = JSONObject().apply {
        put("productId", productId)
        put("payload", payload ?: JSONObject.NULL)
        put("dynamicPriceToken", dynamicPriceToken ?: JSONObject.NULL)
        put("cutoutModeIsShortEdges", cutoutModeIsShortEdges)
    }

    companion object {
        /** Builder function that creates a request with a fresh payload automatically. */
        fun create(productId: String, dynamicPriceToken: String? = null): PurchaseRequest =
            PurchaseRequest(productId, payload = UUID.randomUUID().toString(), dynamicPriceToken = dynamicPriceToken)
    }
}

/** Creates a copy with a new dynamic price token. */
internal fun PurchaseRequest.withDynamicPriceToken(token: String): PurchaseRequest =
    copy(dynamicPriceToken = token)

/** Human-readable one-line summary for logs or UI. */
internal fun PurchaseRequest.summaryLine(): String =
    "🛍 Product: $productId | Payload: ${payload ?: "N/A"} | Token: ${dynamicPriceToken ?: "N/A"} | Cutout: $cutoutModeIsShortEdges"
