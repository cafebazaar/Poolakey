package ir.cafebazaar.poolakey.exception

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Thrown when the Bazaar app (Cafebazaar) is not found on the user's device.
 * This typically occurs when trying to use in-app billing APIs or intents that require Bazaar.
 */
class BazaarNotFoundException(
    val packageName: String = "com.farsitel.bazaar",
    val timestamp: Long = System.currentTimeMillis(),
    val recoveryHint: String? = "Please install or update Bazaar to continue."
) : IllegalStateException() {

    override val message: String?
        get() = "Bazaar is not installed"

    companion object {
        private const val TAG = "BazaarNotFoundException"

        /**
         * Creates a default instance when Bazaar is missing.
         */
        fun create(): BazaarNotFoundException = BazaarNotFoundException()

        /**
         * Returns the official Bazaar install URI.
         */
        fun getInstallUri(): Uri = Uri.parse("bazaar://details?id=com.farsitel.bazaar")

        /**
         * Returns a web fallback URL for Bazaar download.
         */
        fun getWebInstallUri(): Uri =
            Uri.parse("https://cafebazaar.ir/app/com.farsitel.bazaar?l=en")

        /**
         * Checks whether Bazaar is installed on the device.
         */
        fun isBazaarInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo("com.farsitel.bazaar", 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // ===============================================================
    // 📋 Helper Methods
    // ===============================================================

    fun describe(): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        return buildString {
            appendLine("⚠️ BazaarNotFoundException")
            appendLine("Time      : $date")
            appendLine("Message   : ${message ?: "Unknown error"}")
            appendLine("Package   : $packageName")
            appendLine("Hint      : ${recoveryHint ?: "Install Bazaar manually"}")
        }
    }

    fun log(tag: String = TAG) {
        Log.e(tag, "❌ BazaarNotFoundException: ${message}")
        Log.e(tag, "• Package: $packageName")
        Log.e(tag, "• Time   : ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}")
        Log.e(tag, "• Hint   : ${recoveryHint ?: "Install Bazaar manually"}")
    }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject()
        obj.put("error", "BazaarNotFoundException")
        obj.put("message", message)
        obj.put("timestamp", timestamp)
        obj.put("package", packageName)
        obj.put("recoveryHint", recoveryHint)
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun getRecoverySuggestion(): String =
        recoveryHint ?: "Please install or update Bazaar and try again."

    // ===============================================================
    // 🎨 User Interface Helpers
    // ===============================================================

    fun toSpannableMessage(primaryColor: Int, secondaryColor: Int): SpannableString {
        val title = "⚠️ Bazaar Not Installed"
        val body = recoveryHint ?: "Please install Bazaar to continue."
        val fullText = "$title\n\n$body"
        val spannable = SpannableString(fullText)

        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(primaryColor), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val bodyStart = title.length + 2
        if (bodyStart < fullText.length) {
            spannable.setSpan(ForegroundColorSpan(secondaryColor), bodyStart, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    fun showAsToast(context: Context, duration: Int = Toast.LENGTH_LONG) {
        val messageToShow = "⚠️ " + getRecoverySuggestion()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context.applicationContext, messageToShow, duration).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, messageToShow, duration).show()
            }
        }
    }

    /**
     * A beautiful and polished dialog prompting the user to install Bazaar.
     */
    fun showBeautifulDialog(
        context: Context,
        accentColor: Int = ContextCompat.getColor(context, android.R.color.holo_green_dark),
        onInstall: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                showBeautifulDialog(context, accentColor, onInstall, onCancel)
            }
            return
        }

        try {
            val dialogBuilder = AlertDialog.Builder(context)
            val spannableMsg = toSpannableMessage(accentColor, ContextCompat.getColor(context, android.R.color.darker_gray))
            dialogBuilder.setTitle("✨ Bazaar Required ✨")
                .setMessage(spannableMsg)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .setPositiveButton("🚀 Install Bazaar") { dialog, _ ->
                    openBazaarInstallPage(context)
                    onInstall?.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton("❌ Cancel") { dialog, _ ->
                    onCancel?.invoke()
                    dialog.dismiss()
                }
            dialogBuilder.create().show()
        } catch (e: Exception) {
            Log.w(TAG, "Dialog failed: ${e.message}")
            showAsToast(context)
        }
    }

    // ===============================================================
    // 🧠 Behavior & Recovery Functions
    // ===============================================================

    fun attemptRecovery(context: Context): Boolean {
        return if (!isBazaarInstalled(context)) {
            openBazaarInstallPage(context)
            true
        } else false
    }

    fun getLocalizedRecoveryHint(locale: Locale = Locale.getDefault()): String {
        return when (locale.language.lowercase(Locale.ROOT)) {
            "fa" -> "لطفاً برنامه بازار را نصب یا به‌روزرسانی کنید تا بتوانید ادامه دهید."
            else -> recoveryHint ?: "Please install or update Bazaar to continue."
        }
    }

    fun runIfBazaarAvailable(
        context: Context,
        onAvailable: () -> Unit,
        onMissing: (() -> Unit)? = null
    ) {
        if (isBazaarInstalled(context)) onAvailable()
        else onMissing?.invoke() ?: showBeautifulDialog(context)
    }

    fun openBazaarInstallPage(context: Context) {
        val pm: PackageManager = context.packageManager ?: return
        val intent = Intent(Intent.ACTION_VIEW, getInstallUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolved = pm.queryIntentActivities(intent, 0)
        if (resolved?.isNotEmpty() == true) {
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open Bazaar install page: ${e.message}")
            }
        }

        try {
            val webIntent = Intent(Intent.ACTION_VIEW, getWebInstallUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch Bazaar install fallback: ${e.message}")
        }
    }

    fun showInstallToastIfMissing(context: Context, duration: Int = Toast.LENGTH_LONG) {
        if (!isBazaarInstalled(context)) showAsToast(context, duration)
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to "BazaarNotFoundException",
        "message" to message,
        "package" to packageName,
        "timestamp" to timestamp,
        "hint" to recoveryHint
    )

    fun toDebugString(): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        return "[BazaarMissing] ($time) ${message ?: "No message"}"
    }

    override fun toString(): String {
        return "BazaarNotFoundException(packageName=$packageName, message=$message, timestamp=$timestamp)"
    }
}
