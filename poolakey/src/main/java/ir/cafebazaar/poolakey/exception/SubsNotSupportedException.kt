package ir.cafebazaar.poolakey.exception

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.lang.Exception

// Centralized utility function to run code on the main thread
private fun runOnMain(runnable: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) runnable() else Handler(Looper.getMainLooper()).post(runnable)
}

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

    override val message: String get() = "Bazaar is not updated"

    companion object {
        private const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
        fun getUpdateUri() = android.net.Uri.parse("bazaar://details?id=$BAZAAR_PACKAGE")
        fun getWebUpdateUri() = android.net.Uri.parse("https://cafebazaar.ir/app/$BAZAAR_PACKAGE?l=en")
    }

    fun needsUpdate(): Boolean = requiredVersion != null && currentVersion != null && currentVersion < requiredVersion

    fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showUpdateDialog(context: Context, onUpdate: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
        runOnMain {
            try {
                val builder = AlertDialog.Builder(context)
                    .setTitle("⚠️ Bazaar Update Required")
                    .setMessage(recoveryHint)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
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
            context.startActivity(Intent(Intent.ACTION_VIEW, getUpdateUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, getWebUpdateUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {
                showAsToast(context)
            }
        }
    }

    fun showAsToast(context: Context) = runOnMain {
        Toast.makeText(context, recoveryHint ?: "Update Bazaar to continue", Toast.LENGTH_LONG).show()
    }

    fun toJson(pretty: Boolean = true): String = JSONObject().apply {
        put("error", "BazaarNotSupportedException")
        put("package", packageName)
        put("requiredVersion", requiredVersion)
        put("currentVersion", currentVersion)
        put("timestamp", timestamp)
        put("hint", recoveryHint)
    }.let { if (pretty) it.toString(2) else it.toString() }
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

    override val message: String get() = reason ?: "Consume request failed"

    private var retryCount = 0
    private var firstFailureTime = SystemClock.elapsedRealtime()

    fun incrementRetry() { retryCount++ }
    fun isRetryable(maxRetries: Int = 3) = reason?.contains("network", true) == true && retryCount < maxRetries

    fun retryOperationIfNeeded(maxRetries: Int = 3, operation: () -> Unit) {
        if (isRetryable(maxRetries)) {
            incrementRetry()
            runOnMain { Handler().postDelayed({ operation() }, 2000) }
        }
    }

    fun showFriendlyToast(context: Context) = runOnMain {
        Toast.makeText(context, recoverySuggestion(), Toast.LENGTH_LONG).show()
    }

    fun recoverySuggestion(): String = when {
        reason?.contains("network", true) == true -> "Check your internet connection."
        reason?.contains("token", true) == true -> "Purchase token may have expired."
        reason?.contains("timeout", true) == true -> "Try again later."
        else -> "Restart Bazaar and retry."
    }

    fun toJson(pretty: Boolean = true): String = JSONObject().apply {
        put("error", "ConsumeFailedException")
        put("productId", productId)
        put("purchaseToken", purchaseToken)
        put("reason", reason)
        put("timestamp", timestamp)
        put("retryCount", retryCount)
    }.let { if (pretty) it.toString(2) else it.toString() }
}

/* ===============================================================
   3️⃣ DisconnectException
   =============================================================== */
class DisconnectException : IllegalStateException() {
    override val message: String get() = "Bazaar service disconnected"

    fun showEnhancedDialog(context: Context, onRetry: (() -> Unit)? = null) {
        runOnMain {
            AlertDialog.Builder(context)
                .setTitle("⚠️ Service Disconnected")
                .setMessage(message)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Retry 🔄") { dialog, _ ->
                    onRetry?.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton("Dismiss ❌") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    fun toJson(pretty: Boolean = true): String = JSONObject().apply {
        put("error", "DisconnectException")
        put("message", message)
    }.let { if (pretty) it.toString(2) else it.toString() }
}

/* ===============================================================
   4️⃣ DynamicPriceNotSupportedException
   =============================================================== */
class DynamicPriceNotSupportedException : IllegalStateException() {
    override val message: String get() = "Dynamic pricing not supported"

    fun showDialog(context: Context) {
        runOnMain {
            AlertDialog.Builder(context)
                .setTitle("⚠️ Unsupported Feature")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setIcon(android.R.drawable.ic_dialog_info)
                .show()
        }
    }
}

/* ===============================================================
   5️⃣ IAPNotSupportedException
   =============================================================== */
class IAPNotSupportedException : IllegalAccessException() {
    override val message: String? get() = "In-app billing not supported"

    fun notifyUser(context: Context) = runOnMain {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

/* ===============================================================
   6️⃣ PurchaseHijackedException
   =============================================================== */
class PurchaseHijackedException : Exception() {
    override val message: String? get() = "The purchase was hijacked and it's not a valid purchase"
}

/* ===============================================================
   7️⃣ ResultNotOkayException
   =============================================================== */
class ResultNotOkayException : IllegalStateException() {
    override val message: String? get() = "Failed to receive response from Bazaar"
}

/* ===============================================================
   8️⃣ SubsNotSupportedException
   =============================================================== */
class SubsNotSupportedException : IllegalAccessException() {
    override val message: String?
        get() = "Subscription is not supported in this version of installed Bazaar"
}
