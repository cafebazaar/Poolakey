package ir.cafebazaar.poolakey.exception

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Thrown when an operation is intentionally aborted or interrupted.
 * Extends [InterruptedException] for compatibility with thread interruption logic.
 *
 * @property operation The name of the operation that was aborted (optional)
 * @property reason The explanation or cause of abortion (optional)
 * @property timestamp The system time when the abortion occurred.
 */
class AbortedException(
    val operation: String? = null,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    message: String? = null,
    causeThrowable: Throwable? = null
) : InterruptedException(message ?: buildMessage(operation, reason)) {

    init {
        // Attach underlying cause to the Throwable if provided
        causeThrowable?.let { initCause(it) }
    }

    companion object {
        private const val TAG = "AbortedException"

        private fun buildMessage(operation: String?, reason: String?): String {
            return when {
                operation != null && reason != null ->
                    "Operation '$operation' was aborted: $reason"
                operation != null ->
                    "Operation '$operation' was aborted."
                reason != null ->
                    "Operation aborted: $reason"
                else ->
                    "Operation aborted unexpectedly."
            }
        }

        /**
         * Creates an [AbortedException] for user cancellation events.
         */
        fun forUserCancel(operation: String? = null): AbortedException {
            return AbortedException(
                operation = operation,
                reason = "User cancelled the operation.",
                message = "User cancelled ${operation ?: "an operation"}"
            )
        }

        /**
         * Creates an [AbortedException] for timeout scenarios.
         */
        fun forTimeout(operation: String? = null, durationMs: Long? = null): AbortedException {
            val readable = durationMs?.let { "${it / 1000}s" } ?: "unknown"
            return AbortedException(
                operation = operation,
                reason = "Operation timed out after $readable.",
                message = "Timeout occurred during ${operation ?: "an operation"}"
            )
        }

        /**
         * Creates an [AbortedException] due to network issues.
         */
        fun forNetworkError(operation: String? = null): AbortedException {
            return AbortedException(
                operation = operation,
                reason = "Network connection lost or unavailable.",
                message = "Network error during ${operation ?: "operation"}"
            )
        }
    }

    /**
     * Returns a detailed, user-friendly description of the abortion event.
     */
    fun describe(): String {
        return buildString {
            appendLine("⚠️ AbortedException Details ⚠️")
            appendLine("Timestamp : ${formatTimestamp(timestamp)}")
            appendLine("Operation : ${operation ?: "Unknown"}")
            appendLine("Reason    : ${reason ?: "Unspecified"}")
            appendLine("Message   : ${message ?: "No message provided"}")
            this@AbortedException.cause?.let { appendLine("Caused by : ${it.javaClass.simpleName}: ${it.message}") }
        }
    }

    /**
     * Logs this exception in a clean, structured, and visually enhanced way.
     */
    fun log(tag: String = TAG) {
        Log.e(tag, "❌ ${message ?: "AbortedException occurred"}")
        Log.e(tag, "• Operation: ${operation ?: "Unknown"}")
        Log.e(tag, "• Reason   : ${reason ?: "Unspecified"}")
        Log.e(tag, "• Time     : ${formatTimestamp(timestamp)}")
        this.cause?.let {
            Log.e(tag, "• Cause    : ${it.javaClass.simpleName}: ${it.message}")
            Log.d(tag, getStackTraceAsString(it))
        }
    }

    /**
     * Returns true if the abortion reason is due to user cancellation.
     */
    fun isUserCancelled(): Boolean {
        return reason?.contains("cancel", ignoreCase = true) == true
    }

    /**
     * Suggests possible recovery actions based on the abortion context.
     */
    fun getRecoverySuggestion(): String {
        return when {
            isUserCancelled() -> "User canceled the operation. No further action needed."
            reason?.contains("timeout", ignoreCase = true) == true -> "Try increasing the timeout duration and retry."
            reason?.contains("network", ignoreCase = true) == true -> "Please check your internet connection and retry."
            else -> "Check logs or contact support if this issue persists."
        }
    }

    /**
     * Converts the stack trace of the cause (if any) into a string for logging.
     */
    private fun getStackTraceAsString(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    /**
     * Converts the timestamp into a human-readable date/time format.
     */
    private fun formatTimestamp(time: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(time))
    }

    /**
     * Converts this exception into a map structure for analytics or crash reporting.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "operation" to operation,
            "reason" to reason,
            "timestamp" to timestamp,
            "message" to message,
            "isUserCancelled" to isUserCancelled(),
            "recoverySuggestion" to getRecoverySuggestion(),
            "cause" to this.cause?.javaClass?.simpleName
        )
    }

    /**
     * Converts this exception to JSON for structured logging or remote reporting.
     * Uses JSONObject to ensure proper escaping of strings.
     */
    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject()
        obj.put("operation", operation ?: JSONObject.NULL)
        obj.put("reason", reason ?: JSONObject.NULL)
        obj.put("timestamp", timestamp)
        obj.put("message", message ?: JSONObject.NULL)
        obj.put("isUserCancelled", isUserCancelled())
        obj.put("recoverySuggestion", getRecoverySuggestion())
        obj.put("cause", this.cause?.javaClass?.simpleName ?: JSONObject.NULL)
        return if (pretty) obj.toString(2) else obj.toString()
    }

    override fun toString(): String {
        return "AbortedException(operation=$operation, reason=$reason, timestamp=$timestamp, message=$message)"
    }

    // =======================================================
    // 🎨 UI Helpers — these make presenting the exception more pleasant
    // (lightweight helpers that don't force UI dependencies)
    // =======================================================

    /**
     * Returns a styled [SpannableString] suitable for showing in a TextView.
     * Title (operation) will be bold and primary colored; body will be secondary colored; time will be dim + italic.
     *
     * Example usage:
     * textView.text = abortedException.toSpannableMessage(primaryColor, secondaryColor)
     */
    fun toSpannableMessage(primaryColor: Int, secondaryColor: Int): SpannableString {
        val title = operation ?: "Operation aborted"
        val body = reason ?: (message ?: "The operation was aborted.")
        val time = formatTimestamp(timestamp)
        val full = "$title\n$body\n$time"
        val spannable = SpannableString(full)

        // title bold + primary color
        spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(primaryColor), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // body colored secondary
        val bodyStart = title.length + 1
        val bodyEnd = bodyStart + body.length
        if (bodyStart < bodyEnd) {
            spannable.setSpan(ForegroundColorSpan(secondaryColor), bodyStart, bodyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // time dim + italic
        val timeStart = bodyEnd + 1
        val timeEnd = full.length
        if (timeStart < timeEnd) {
            spannable.setSpan(ForegroundColorSpan(adjustAlpha(secondaryColor, 0.7f)), timeStart, timeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(android.graphics.Typeface.ITALIC), timeStart, timeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    private fun adjustAlpha(@androidx.annotation.ColorInt color: Int, factor: Float): Int {
        val a = (android.graphics.Color.alpha(color) * factor).toInt()
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(a, r, g, b)
    }

    /**
     * Shows a short Toast to the user with a friendly message extracted from this exception.
     * Safe to call from any thread (it will post to main Looper).
     */
    fun showAsToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
        val toastText = when {
            isUserCancelled() -> "Action cancelled."
            reason != null -> reason
            message != null -> message
            else -> "Operation aborted."
        } ?: "Operation aborted."

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context.applicationContext, toastText, duration).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, toastText, duration).show()
            }
        }
    }
}
