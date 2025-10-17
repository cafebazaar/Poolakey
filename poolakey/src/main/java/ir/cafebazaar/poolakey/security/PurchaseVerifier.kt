package ir.cafebazaar.poolakey.security

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Base64
import android.util.Log
import android.widget.TextView
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

// =======================================================
// 🔐 Secure Purchase Verifier (Ultimate Extended Version)
// =======================================================

internal class PurchaseVerifier {

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IllegalArgumentException::class
    )
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        val key = generatePublicKey(base64PublicKey)
        return verify(key, signedData, signature)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        IllegalArgumentException::class
    )
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        // normalize input to remove spaces/newlines that sometimes appear in keys
        val normalized = normalizeBase64Input(encodedPublicKey)
        val decodedKey = Base64.decode(normalized, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
        signatureAlgorithm.initVerify(publicKey)
        signatureAlgorithm.update(signedData.toByteArray(StandardCharsets.UTF_8))
        return signatureAlgorithm.verify(Base64.decode(signature, Base64.DEFAULT))
    }

    companion object {
        private const val KEY_FACTORY_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

        // =======================================================
        // 🆕 Extended & Advanced Utility Functions
        // =======================================================

        /**
         * Enhanced verification using SHA256 fallback.
         */
        fun verifySecure(
            base64PublicKey: String,
            signedData: String,
            signature: String
        ): VerificationResult {
            return try {
                val key = generateKey(base64PublicKey)
                val verifiedSHA1 = tryVerify(key, signedData, signature, "SHA1withRSA")
                val verifiedSHA256 = verifiedSHA1 || tryVerify(key, signedData, signature, "SHA256withRSA")

                when {
                    verifiedSHA256 -> VerificationResult(success = true, algorithm = "SHA256withRSA")
                    verifiedSHA1 -> VerificationResult(success = true, algorithm = "SHA1withRSA")
                    else -> VerificationResult(success = false, errorMessage = "Signature verification failed.")
                }
            } catch (e: Exception) {
                Log.e("PurchaseVerifier", "Verification error: ${e.message}")
                VerificationResult(success = false, errorMessage = e.localizedMessage ?: "Unknown error")
            }
        }

        private fun generateKey(encodedPublicKey: String): PublicKey {
            val normalized = normalizeBase64Input(encodedPublicKey)
            if (!isBase64Valid(normalized)) {
                throw IllegalArgumentException("Invalid Base64 key format")
            }
            val decodedKey = Base64.decode(normalized, Base64.DEFAULT)
            val factory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            return factory.generatePublic(X509EncodedKeySpec(decodedKey))
        }

        private fun tryVerify(
            publicKey: PublicKey,
            signedData: String,
            signature: String,
            algorithm: String
        ): Boolean {
            return try {
                val verifier = Signature.getInstance(algorithm)
                verifier.initVerify(publicKey)
                verifier.update(signedData.toByteArray(StandardCharsets.UTF_8))
                verifier.verify(Base64.decode(signature, Base64.DEFAULT))
            } catch (_: Exception) {
                false
            }
        }

        fun isBase64Valid(data: String): Boolean {
            return try {
                Base64.decode(data, Base64.DEFAULT)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        fun generateDiagnosticReport(
            publicKey: String,
            signedData: String,
            signature: String
        ): Map<String, Any> {
            val normalized = normalizeBase64Input(publicKey)
            val base64Valid = isBase64Valid(normalized)
            val report = mutableMapOf<String, Any>(
                "base64Valid" to base64Valid,
                "signedDataLength" to signedData.length,
                "signatureLength" to signature.length,
                "timestamp" to System.currentTimeMillis()
            )
            if (!base64Valid) report["error"] = "Invalid Base64 key format"
            return report
        }

        data class VerificationResult(
            val success: Boolean,
            val algorithm: String? = null,
            val errorMessage: String? = null,
            val timestamp: Long = System.currentTimeMillis()
        ) {
            fun toPrettyString(): String {
                return if (success) {
                    "✅ Purchase verified successfully using $algorithm at $timestamp"
                } else {
                    "❌ Verification failed: ${errorMessage ?: "Unknown error"}"
                }
            }

            /** UI label — short */
            fun uiLabel(): String = if (success) "Verified" else "Not Verified"

            /** Suggested color for UI badge */
            fun uiColor(): Int = if (success) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        }

        fun encodeToBase64(data: ByteArray): String =
            Base64.encodeToString(data, Base64.NO_WRAP)

        fun decodeFromBase64(data: String): ByteArray? =
            try { Base64.decode(normalizeBase64Input(data), Base64.DEFAULT) } catch (_: Exception) { null }

        fun isKeyValid(encodedKey: String): Boolean {
            return try {
                val key = generateKey(encodedKey)
                key.algorithm == KEY_FACTORY_ALGORITHM
            } catch (_: Exception) {
                false
            }
        }

        // =======================================================
        // 🧩 NEW FUNCTIONS FOR SECURITY AND DIAGNOSTICS
        // =======================================================

        /**
         * Returns a SHA-256 fingerprint of a public key for easy auditing/logging.
         */
        fun getPublicKeyFingerprint(encodedPublicKey: String): String? {
            return try {
                val keyBytes = Base64.decode(normalizeBase64Input(encodedPublicKey), Base64.DEFAULT)
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(keyBytes)
                hash.joinToString(":") { "%02X".format(it) }
            } catch (e: Exception) {
                Log.e("PurchaseVerifier", "Fingerprint error: ${e.message}")
                null
            }
        }

        /**
         * Encrypts data using the provided RSA public key.
         * This can be useful for securely transmitting app data.
         */
        fun encryptWithPublicKey(data: String, base64PublicKey: String): String? {
            return try {
                val publicKey = generateKey(base64PublicKey)
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
                encodeToBase64(encryptedBytes)
            } catch (e: Exception) {
                Log.e("PurchaseVerifier", "Encryption failed: ${e.message}")
                null
            }
        }

        /**
         * Verifies if two signatures are equivalent across different algorithms (SHA1 vs SHA256).
         * Useful for cross-version signature migration.
         */
        fun compareSignatureAlgorithms(
            base64PublicKey: String,
            signedData: String,
            signatureSHA1: String,
            signatureSHA256: String
        ): Boolean {
            return try {
                val key = generateKey(base64PublicKey)
                val sha1Valid = tryVerify(key, signedData, signatureSHA1, "SHA1withRSA")
                val sha256Valid = tryVerify(key, signedData, signatureSHA256, "SHA256withRSA")
                sha1Valid && sha256Valid
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Performs a complete diagnostic test of a purchase verification flow.
         */
        fun runFullDiagnostic(
            base64PublicKey: String,
            signedData: String,
            signature: String
        ): String {
            val report = generateDiagnosticReport(base64PublicKey, signedData, signature)
            val fingerprint = getPublicKeyFingerprint(base64PublicKey) ?: "Unavailable"
            val verification = verifySecure(base64PublicKey, signedData, signature)
            return buildString {
                appendLine("🔍 Poolakey Verification Diagnostic")
                appendLine("===================================")
                appendLine("Public Key Fingerprint: $fingerprint")
                appendLine("Base64 Valid: ${report["base64Valid"]}")
                appendLine("Signed Data Length: ${report["signedDataLength"]}")
                appendLine("Signature Length: ${report["signatureLength"]}")
                appendLine("Verification Result: ${verification.toPrettyString()}")
                appendLine("Timestamp: ${report["timestamp"]}")
                if (report.containsKey("error")) {
                    appendLine("⚠️ Error: ${report["error"]}")
                }
            }
        }

        /**
         * Sanitizes and normalizes Base64 input by removing unwanted whitespace or line breaks.
         */
        fun normalizeBase64Input(data: String): String {
            return data.replace("\\s".toRegex(), "")
        }

        /**
         * Returns a simplified verification boolean with internal error safety.
         */
        fun quickVerify(base64PublicKey: String, signedData: String, signature: String): Boolean {
            return verifySecure(base64PublicKey, signedData, signature).success
        }
    }
}

// =======================================================
// 🖼 UI Helpers (visual improvements for verification output)
// =======================================================

/**
 * Apply a visually appealing badge to a TextView to show verification result.
 * - sets text to `result.uiLabel()` (e.g., "Verified" / "Not Verified")
 * - sets a rounded gradient background and appropriate text color
 * - optional small label (algorithm or message) appended
 */
fun styleVerificationBadge(textView: TextView, result: PurchaseVerifier.Companion.VerificationResult, context: Context, smallLabel: String? = null) {
    val bgColor = if (result.success) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
    val start = adjustAlpha(bgColor, 1.12f)
    val end = adjustAlpha(bgColor, 0.90f)

    val grad = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(start, end)
    ).apply {
        cornerRadius = dp(context, 18f)
    }

    textView.apply {
        text = if (smallLabel.isNullOrBlank()) result.uiLabel() else "${result.uiLabel()} • $smallLabel"
        setTextColor(Color.WHITE)
        setTypeface(Typeface.DEFAULT_BOLD)
        textSize = 14f
        setPadding(dp(context, 14f).toInt(), dp(context, 8f).toInt(), dp(context, 14f).toInt(), dp(context, 8f).toInt())
        background = grad
        elevation = 6f
    }
}

/** small helpers for UI */
private fun dp(context: Context, v: Float): Float = v * context.resources.displayMetrics.density

private fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
    val r = (Color.red(color) * factor).coerceIn(0f, 255f).toInt()
    val g = (Color.green(color) * factor).coerceIn(0f, 255f).toInt()
    val b = (Color.blue(color) * factor).coerceIn(0f, 255f).toInt()
    val a = Color.alpha(color)
    return Color.argb(a, r, g, b)
} 
