package ir.cafebazaar.poolakey.exception

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
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

    override val message: String?
        get() = "Bazaar is not updated"

    companion object {
        private const val TAG = "BazaarNotSupportedException"
        private const val BAZAAR_PACKAGE = "com.farsitel.bazaar"

        fun getUpdateUri(): Uri = Uri.parse("bazaar://details?id=$BAZAAR_PACKAGE")
        fun getWebUpdateUri(): Uri = Uri.parse("https://cafebazaar.ir/app/$BAZAAR_PACKAGE?l=en")

        fun isBazaarInstalled(context: Context): Boolean =
            try { context.packageManager.getPackageInfo(BAZAAR_PACKAGE, 0); true } catch (e: Exception) { false }

        fun getInstalledVersionCode(context: Context): Long? =
            try {
                val info = context.packageManager.getPackageInfo(BAZAAR_PACKAGE, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            } catch (e: Exception) { null }

        fun isVersionSupported(context: Context, requiredVersion: Int): Boolean {
            val current = getInstalledVersionCode(context) ?: return false
            return current >= requiredVersion
        }
    }

    fun showAsToast(context: Context) {
        runOnMain { Toast.makeText(context, "⚠️ ${getRecoveryHint()}", Toast.LENGTH_LONG).show() }
    }

    fun showUpdateDialog(context: Context, onUpdate: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
        val uiAction = {
            try {
                val builder = AlertDialog.Builder(context)
                    .setTitle("⚠️ Bazaar Update Required")
                    .setMessage("Your Bazaar app is outdated.\n\n${getRecoveryHint()}")
                    .setCancelable(false)
                    .setPositiveButton("Update Now 🚀") { dialog, _ ->
                        openBazaarUpdatePage(context); onUpdate?.invoke(); dialog.dismiss()
                    }
                    .setNegativeButton("Cancel ❌") { dialog, _ -> onCancel?.invoke(); dialog.dismiss() }

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
                dialog.show()
            } catch (e: Exception) {
                Log.w(TAG, "Dialog failed: ${e.message}")
                showAsToast(context)
            }
        }
        runOnMain(uiAction)
    }

    fun openBazaarUpdatePage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, getUpdateUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, getWebUpdateUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open web update page: ${ex.message}")
                showAsToast(context)
            }
        }
    }

    fun getRecoveryHint(): String = recoveryHint ?: "Please update Bazaar to continue."
    fun runOnMain(runnable: () -> Unit) { if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable) }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply {
            put("error", "BazaarNotSupportedException")
            put("package", packageName)
            put("requiredVersion", requiredVersion)
            put("currentVersion", currentVersion)
            put("timestamp", timestamp)
            put("hint", recoveryHint)
        }
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to "BazaarNotSupportedException",
        "package" to packageName,
        "requiredVersion" to requiredVersion,
        "currentVersion" to currentVersion,
        "timestamp" to timestamp,
        "hint" to recoveryHint
    )
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

    override val message: String? get() = reason ?: "Consume request failed: It's from Bazaar"

    companion object {
        fun fromNetworkError() = ConsumeFailedException(reason = "Network connection error")
        fun fromInvalidToken(token: String) = ConsumeFailedException(purchaseToken = token, reason = "Invalid or expired purchase token")
        fun fromTimeout() = ConsumeFailedException(reason = "Consume request timed out")
        fun fromUnknownError() = ConsumeFailedException(reason = "Unknown internal error")
    }

    private var retryCount: Int = 0
    private var firstFailureTime: Long = SystemClock.elapsedRealtime()

    fun incrementRetry() { retryCount++ }
    fun getRetryCount(): Int = retryCount
    fun resetRetryTracking() { retryCount = 0; firstFailureTime = SystemClock.elapsedRealtime() }

    fun isTemporary(): Boolean = reason?.contains("network", true) == true || reason?.contains("timeout", true) == true
    fun isPermanent(): Boolean = reason?.contains("token", true) == true || reason?.contains("invalid", true) == true
    fun shouldRetry(maxRetries: Int = 3): Boolean = isTemporary() && retryCount < maxRetries

    fun getRecoverySuggestion(): String = when {
        reason?.contains("network", true) == true -> "Check your internet connection."
        reason?.contains("token", true) == true -> "Purchase token may have expired."
        reason?.contains("timeout", true) == true -> "Try increasing timeout or retrying later."
        else -> "Restart Bazaar and retry the operation."
    }

    fun showFriendlyToast(context: Context) { runOnMain { Toast.makeText(context, "⚠️ ${getRecoverySuggestion()}", Toast.LENGTH_LONG).show() } }
    private fun runOnMain(runnable: () -> Unit) { if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable) }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply {
            put("error", "ConsumeFailedException")
            put("productId", productId)
            put("purchaseToken", purchaseToken)
            put("reason", reason)
            put("timestamp", timestamp)
            put("retryCount", retryCount)
            put("elapsedSeconds", (SystemClock.elapsedRealtime() - firstFailureTime)/1000)
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
}

/* ===============================================================
   3️⃣ DisconnectException
   =============================================================== */
class DisconnectException : IllegalStateException() {

    override val message: String? get() = "We can't communicate with Bazaar: Service is disconnected"

    fun showAsToast(context: Context) { Toast.makeText(context, "⚠️ $message", Toast.LENGTH_LONG).show() }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply { put("error", "DisconnectException"); put("message", message) }
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun toMap(): Map<String, Any?> = mapOf("type" to "DisconnectException", "message" to message)
}
