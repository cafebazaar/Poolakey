package ir.cafebazaar.poolakey.exception

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/* ===============================================================
   1️⃣ BazaarNotSupportedException
   =============================================================== */
class BazaarNotSupportedException(
    val packageName: String = "com.farsitel.bazaar",
    val requiredVersion: Int? = null,
    val currentVersion: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val recoveryHint: String? = "Please update Bazaar to the latest version to continue."
) : IllegalStateException() {

    override val message: String
        get() = "Bazaar is not updated"

    companion object {
        private const val BAZAAR_PACKAGE = "com.farsitel.bazaar"

        fun getUpdateUri(): android.net.Uri = android.net.Uri.parse("bazaar://details?id=$BAZAAR_PACKAGE")
        fun getWebUpdateUri(): android.net.Uri = android.net.Uri.parse("https://cafebazaar.ir/app/$BAZAAR_PACKAGE?l=en")
    }

    fun needsUpdate(): Boolean = requiredVersion != null && currentVersion != null && currentVersion < requiredVersion

    fun elapsedSinceThrown(): Long = (System.currentTimeMillis() - timestamp) / 1000

    fun getReadableElapsedTime(): String {
        val seconds = elapsedSinceThrown()
        val minutes = seconds / 60
        val remaining = seconds % 60
        return if (minutes > 0) "${minutes}m ${remaining}s ago" else "${remaining}s ago"
    }

    fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showAsToast(context: Context) {
        runOnMain { Toast.makeText(context, getRecoveryHint(), Toast.LENGTH_LONG).show() }
    }

    fun showUpdateDialog(context: Context, onUpdate: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
        runOnMain {
            try {
                val builder = AlertDialog.Builder(context)
                    .setTitle("⚠️ Bazaar Update Required")
                    .setMessage(getRecoveryHint())
                    .setCancelable(false)
                    .setPositiveButton("Update Now 🚀") { dialog, _ ->
                        openBazaarUpdatePage(context)
                        onUpdate?.invoke()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel ❌") { dialog, _ ->
                        onCancel?.invoke()
                        dialog.dismiss()
                    }

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
                dialog.show()
            } catch (e: Exception) {
                showAsToast(context)
            }
        }
    }

    fun openBazaarUpdatePage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, getUpdateUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, getWebUpdateUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                showAsToast(context)
            }
        }
    }

    fun getRecoveryHint(): String = recoveryHint ?: "Please update Bazaar to continue."

    fun diagnosticReport(context: Context): String = buildString {
        appendLine("=== BazaarNotSupportedException Diagnostic ===")
        appendLine("Package         : $packageName")
        appendLine("Required Version: ${requiredVersion ?: "Unknown"}")
        appendLine("Current Version : ${currentVersion ?: "Unknown"}")
        appendLine("Hint            : ${getRecoveryHint()}")
        appendLine("Elapsed Time    : ${getReadableElapsedTime()}")
        appendLine("Internet        : ${if (hasInternetConnection(context)) "Available" else "Unavailable"}")
        appendLine("==============================================")
    }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply {
            put("error", "BazaarNotSupportedException")
            put("package", packageName)
            put("requiredVersion", requiredVersion)
            put("currentVersion", currentVersion)
            put("timestamp", timestamp)
            put("hint", getRecoveryHint())
        }
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to "BazaarNotSupportedException",
        "package" to packageName,
        "requiredVersion" to requiredVersion,
        "currentVersion" to currentVersion,
        "timestamp" to timestamp,
        "hint" to getRecoveryHint()
    )

    private fun runOnMain(runnable: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable)
    }
}

/* ===============================================================
   2️⃣ ConsumeFailedException
   =============================================================== */
class ConsumeFailedException(
    val productId: String? = null,
    val purchaseToken: String? = null,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : RemoteException() {

    override val message: String
        get() = reason ?: "Consume request failed: It's from Bazaar"

    private var retryCount: Int = 0
    private var firstFailureTime: Long = SystemClock.elapsedRealtime()

    fun incrementRetry() { retryCount++ }
    fun getRetryCount(): Int = retryCount
    fun resetRetryTracking() { retryCount = 0; firstFailureTime = SystemClock.elapsedRealtime() }

    fun timeSinceFirstFailureSeconds(): Long = (SystemClock.elapsedRealtime() - firstFailureTime) / 1000

    fun getReadableTimeSinceFailure(): String {
        val seconds = timeSinceFirstFailureSeconds()
        val minutes = seconds / 60
        val remaining = seconds % 60
        return if (minutes > 0) "${minutes}m ${remaining}s ago" else "${remaining}s ago"
    }

    fun isTemporary(): Boolean = reason?.contains("network", true) == true || reason?.contains("timeout", true) == true
    fun isPermanent(): Boolean = reason?.contains("token", true) == true || reason?.contains("invalid", true) == true
    fun isRetryable(maxRetries: Int = 3): Boolean = isTemporary() && retryCount < maxRetries

    fun getRecoverySuggestion(): String = when {
        reason?.contains("network", true) == true -> "Check your internet connection."
        reason?.contains("token", true) == true -> "Purchase token may have expired."
        reason?.contains("timeout", true) == true -> "Try increasing timeout or retrying later."
        else -> "Restart Bazaar and retry the operation."
    }

    fun showFriendlyToast(context: Context) {
        runOnMain { Toast.makeText(context, getRecoverySuggestion(), Toast.LENGTH_LONG).show() }
    }

    fun fullDiagnosticReport(): String = buildString {
        appendLine("=== ConsumeFailedException Diagnostic ===")
        appendLine("Product ID      : ${productId ?: "Unknown"}")
        appendLine("Purchase Token  : ${purchaseToken ?: "Unknown"}")
        appendLine("Reason          : ${reason ?: "Unknown"}")
        appendLine("Retry Count     : $retryCount")
        appendLine("Elapsed Time    : ${getReadableTimeSinceFailure()}")
        appendLine("Temporary       : ${isTemporary()}")
        appendLine("Permanent       : ${isPermanent()}")
        appendLine("Recovery Hint   : ${getRecoverySuggestion()}")
        appendLine("=========================================")
    }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply {
            put("error", "ConsumeFailedException")
            put("productId", productId)
            put("purchaseToken", purchaseToken)
            put("reason", reason)
            put("timestamp", timestamp)
            put("retryCount", retryCount)
            put("elapsedSeconds", timeSinceFirstFailureSeconds())
        }
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to "ConsumeFailedException",
        "productId" to productId,
        "purchaseToken" to purchaseToken,
        "reason" to reason,
        "timestamp" to timestamp,
        "retryCount" to retryCount
    )

    private fun runOnMain(runnable: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable)
    }

    companion object {
        fun fromNetworkError() = ConsumeFailedException(reason = "Network connection error")
        fun fromInvalidToken(token: String) = ConsumeFailedException(purchaseToken = token, reason = "Invalid or expired purchase token")
        fun fromTimeout() = ConsumeFailedException(reason = "Consume request timed out")
        fun fromUnknownError() = ConsumeFailedException(reason = "Unknown internal error")
    }
}

/* ===============================================================
   3️⃣ DisconnectException
   =============================================================== */
class DisconnectException : IllegalStateException() {

    override val message: String
        get() = "We can't communicate with Bazaar: Service is disconnected"

    fun showAsToast(context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showEnhancedDialog(context: Context, onRetry: (() -> Unit)? = null) {
        runOnMain {
            try {
                val builder = AlertDialog.Builder(context)
                    .setTitle("⚠️ Bazaar Service Disconnected")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Retry 🔄") { dialog, _ ->
                        onRetry?.invoke()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Dismiss ❌") { dialog, _ ->
                        dialog.dismiss()
                    }
                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
                dialog.show()
            } catch (e: Exception) {
                showAsToast(context)
            }
        }
    }

    fun isRecoverable(): Boolean = true

    fun toJson(pretty: Boolean = true): String = JSONObject().apply {
        put("error", "DisconnectException")
        put("message", message)
    }.let { if (pretty) it.toString(2) else it.toString() }

    fun toMap(): Map<String, Any?> = mapOf("type" to "DisconnectException", "message" to message)

    fun toAnalyticsMap(): Map<String, String> = mapOf("type" to "DisconnectException", "message" to message, "timestamp" to System.currentTimeMillis().toString())

    private fun runOnMain(runnable: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable)
    }
}

/* ===============================================================
   4️⃣ DynamicPriceNotSupportedException
   =============================================================== */
class DynamicPriceNotSupportedException : IllegalStateException() {

    override val message: String
        get() = "Dynamic price not supported"

    fun showDialog(context: Context) {
        runOnMain {
            try {
                AlertDialog.Builder(context)
                    .setTitle("⚠️ Unsupported Feature")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun toAnalyticsMap(): Map<String, String> = mapOf("type" to "DynamicPriceNotSupportedException", "message" to message, "timestamp" to System.currentTimeMillis().toString())

    private fun runOnMain(runnable: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable)
    }
}
