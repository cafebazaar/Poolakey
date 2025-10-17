package ir.cafebazaar.poolakey.exception

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

/**
 * Thrown when a "consume" request to Bazaar fails,
 * typically due to a network, billing, or remote service issue.
 */
class ConsumeFailedException(
    val productId: String? = null,
    val purchaseToken: String? = null,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : RemoteException() {

    override val message: String?
        get() = reason ?: "Consume request failed: It's from Bazaar"

    companion object {
        private const val TAG = "ConsumeFailedException"

        /**
         * Creates a preconfigured exception for a known common cause.
         */
        fun fromNetworkError(): ConsumeFailedException {
            return ConsumeFailedException(reason = "Network connection error")
        }

        fun fromInvalidToken(token: String): ConsumeFailedException {
            return ConsumeFailedException(
                purchaseToken = token,
                reason = "Invalid or expired purchase token"
            )
        }

        fun fromTimeout(): ConsumeFailedException {
            return ConsumeFailedException(reason = "Consume request timed out")
        }

        fun fromUnknownError(): ConsumeFailedException {
            return ConsumeFailedException(reason = "Unknown internal error")
        }
    }

    // ===============================================================
    // 📋 Diagnostic & Logging Tools
    // ===============================================================

    fun describe(): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
        return buildString {
            appendLine("⚠️ ConsumeFailedException")
            appendLine("Time       : $date")
            appendLine("Product ID : ${productId ?: "Unknown"}")
            appendLine("Token      : ${purchaseToken ?: "Unknown"}")
            appendLine("Reason     : ${reason ?: "Unknown failure"}")
            appendLine("Message    : ${message}")
        }
    }

    fun log(tag: String = TAG, detailed: Boolean = false) {
        Log.e(tag, "❌ ConsumeFailedException: ${message}")
        if (detailed) {
            Log.e(tag, describe())
        } else {
            if (productId != null) Log.e(tag, "• Product ID: $productId")
            if (purchaseToken != null) Log.e(tag, "• Token: $purchaseToken")
            Log.e(tag, "• Timestamp: $timestamp")
        }
    }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply {
            put("error", "ConsumeFailedException")
            put("message", message)
            put("productId", productId)
            put("purchaseToken", purchaseToken)
            put("reason", reason)
            put("timestamp", timestamp)
            put("retryCount", retryCount)
            put("elapsedSeconds", timeSinceFirstFailure())
        }
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to "ConsumeFailedException",
        "message" to message,
        "productId" to productId,
        "purchaseToken" to purchaseToken,
        "reason" to reason,
        "timestamp" to timestamp,
        "retryCount" to retryCount,
        "elapsedSeconds" to timeSinceFirstFailure()
    )

    // ===============================================================
    // 🎨 User Feedback Helpers
    // ===============================================================

    /**
     * Safely show a toast on main thread.
     */
    fun showAsToast(context: Context, duration: Int = Toast.LENGTH_LONG) {
        val msg = message ?: "Consume operation failed."
        runOnMain {
            Toast.makeText(context.applicationContext, msg, duration).show()
        }
    }

    fun getReadableError(): String {
        return when {
            reason?.contains("network", true) == true ->
                "Network issue detected. Please check your connection."
            reason?.contains("timeout", true) == true ->
                "The operation took too long. Please try again."
            reason?.contains("token", true) == true ->
                "Purchase token invalid. Please refresh your purchase list."
            else -> "Unable to complete purchase consumption. Try again later."
        }
    }

    fun showFriendlyToast(context: Context, duration: Int = Toast.LENGTH_LONG) {
        runOnMain {
            Toast.makeText(context.applicationContext, getReadableError(), duration).show()
        }
    }

    /**
     * Shows a retry dialog (main thread). Buttons styled using system colors.
     * - onRetry: invoked when user chooses to retry
     * - onCancel: invoked on cancel
     */
    fun showRetryDialog(
        context: Context,
        title: String = "Consume Failed",
        messageText: String = getReadableError(),
        positiveLabel: String = "Retry",
        negativeLabel: String = "Cancel",
        onRetry: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        runOnMain {
            try {
                val builder = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(messageText)
                    .setCancelable(true)
                    .setPositiveButton(positiveLabel) { dialog, _ ->
                        onRetry?.invoke()
                        dialog.dismiss()
                    }
                    .setNegativeButton(negativeLabel) { dialog, _ ->
                        onCancel?.invoke()
                        dialog.dismiss()
                    }

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let {
                        try {
                            it.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                        } catch (_: Exception) { /* ignore */ }
                    }
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let {
                        try {
                            it.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                        } catch (_: Exception) { /* ignore */ }
                    }
                }
                dialog.show()
            } catch (e: Exception) {
                Log.w(TAG, "Dialog failed: ${e.message}")
                showFriendlyToast(context)
            }
        }
    }

    // ===============================================================
    // ⏱ Timing & Retry Utilities
    // ===============================================================

    // protected by 'synchronized' on methods that mutate them
    private var retryCount: Int = 0
    private var firstFailureTime: Long = SystemClock.elapsedRealtime()

    @Synchronized
    fun incrementRetry() {
        retryCount++
    }

    @Synchronized
    fun getRetryCount(): Int = retryCount

    @Synchronized
    fun resetRetryTracking() {
        retryCount = 0
        firstFailureTime = SystemClock.elapsedRealtime()
    }

    fun timeSinceFirstFailure(): Long =
        (SystemClock.elapsedRealtime() - firstFailureTime) / 1000

    fun getReadableElapsedTime(): String {
        val seconds = timeSinceFirstFailure()
        val minutes = seconds / 60
        val remaining = seconds % 60
        return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s"
    }

    // ===============================================================
    // 🩺 Recovery & Resilience Helpers
    // ===============================================================

    /**
     * Suggests a recovery action based on the cause.
     */
    fun getRecoverySuggestion(): String {
        return when {
            reason?.contains("network", true) == true ->
                "Please ensure your device is connected to the internet."
            reason?.contains("token", true) == true ->
                "The purchase token may have expired. Requery or refresh the purchase list."
            reason?.contains("timeout", true) == true ->
                "Try increasing timeout or retrying later."
            else -> "Try restarting the Bazaar app and retry the operation."
        }
    }

    /**
     * Returns `true` if this failure is likely temporary and worth retrying.
     */
    fun isTemporary(): Boolean {
        return reason?.contains("network", true) == true ||
                reason?.contains("timeout", true) == true
    }

    /**
     * Returns `true` if this failure is likely permanent (e.g., invalid token).
     */
    fun isPermanent(): Boolean {
        return reason?.contains("token", true) == true ||
                reason?.contains("invalid", true) == true
    }

    /**
     * Suggests whether a retry should be attempted.
     */
    fun shouldRetry(maxRetries: Int = 3): Boolean {
        return isTemporary() && getRetryCount() < maxRetries
    }

    // ===============================================================
    // 📊 Analytics & Summary
    // ===============================================================

    fun toAnalyticsBundle(): Map<String, String> = mapOf(
        "exception" to "ConsumeFailedException",
        "product_id" to (productId ?: "unknown"),
        "reason" to (reason ?: "unknown"),
        "elapsed_seconds" to timeSinceFirstFailure().toString(),
        "timestamp" to timestamp.toString(),
        "retries" to getRetryCount().toString()
    )

    fun compactSummary(): String =
        "ConsumeFailed(product=${productId ?: "?"}, reason=${reason ?: "unknown"}, retries=${getRetryCount()})"

    fun diagnosticReport(): String = buildString {
        appendLine("==== Consume Failure Diagnostic ====")
        appendLine(describe())
        appendLine("Retry Count : ${getRetryCount()}")
        appendLine("Elapsed Time: ${getReadableElapsedTime()}")
        appendLine("Temporary   : ${isTemporary()}")
        appendLine("Permanent   : ${isPermanent()}")
        appendLine("Suggestion  : ${getRecoverySuggestion()}")
        appendLine("====================================")
    }

    // ===============================================================
    // 🧰 Utility
    // ===============================================================

    /**
     * Converts this exception into a human-readable log block.
     */
    fun prettyPrint(): String {
        return """
            |🚨 ConsumeFailedException
            |Product ID : ${productId ?: "N/A"}
            |Reason     : ${reason ?: "Unknown"}
            |Retries    : ${getRetryCount()}
            |Temporary  : ${isTemporary()}
            |Timestamp  : $timestamp
            |Message    : $message
        """.trimMargin()
    }

    /**
     * Emits this exception as a standardized debug message.
     */
    fun debugLog(tag: String = TAG) {
        Log.w(tag, prettyPrint())
    }

    // ===============================================================
    // 🔁 Async Retry Helper (suspend) with exponential backoff
    // ===============================================================
    /**
     * Attempts the provided suspend [action] repeatedly with exponential backoff until it returns true
     * or until [maxAttempts] is reached. Returns the success boolean and increments retry counter.
     *
     * Example usage:
     * ```
     * val success = consumeFailedException.retryWithBackoff({
     *     // attempt retry logic (suspend) -> Boolean
     * }, maxAttempts = 4, baseDelayMs = 500L)
     * ```
     */
    suspend fun retryWithBackoff(
        action: suspend () -> Boolean,
        maxAttempts: Int = 4,
        baseDelayMs: Long = 500L
    ): Boolean {
        resetRetryTracking()
        repeat(maxAttempts) { attempt ->
            val ok = try {
                action()
            } catch (e: Exception) {
                false
            }
            if (ok) {
                return true
            } else {
                incrementRetry()
                // exponential backoff (2^attempt * baseDelay)
                val delayMs = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
                delay(delayMs)
            }
        }
        return false
    }

    // ===============================================================
    // Helper: post to main looper safely
    // ===============================================================
    private fun runOnMain(runnable: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post { runnable() }
    }

    override fun toString(): String {
        return "ConsumeFailedException(productId=$productId, purchaseToken=$purchaseToken, reason=$reason, message=$message, timestamp=$timestamp)"
    }
}
