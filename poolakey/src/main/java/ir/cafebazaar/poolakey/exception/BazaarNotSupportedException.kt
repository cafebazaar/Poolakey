package ir.cafebazaar.poolakey.exception

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exception thrown when Bazaar app is installed but not updated
 * to a version that supports the required billing or service features.
 */
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

        /** Bazaar in-app URI */
        fun getUpdateUri(): Uri = Uri.parse("bazaar://details?id=$BAZAAR_PACKAGE")

        /** Web fallback URI */
        fun getWebUpdateUri(): Uri =
            Uri.parse("https://cafebazaar.ir/app/$BAZAAR_PACKAGE?l=en")

        /** Check if Bazaar is installed */
        fun isBazaarInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo(BAZAAR_PACKAGE, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
        }

        /** Get installed Bazaar version code (supports all API levels) */
        fun getInstalledVersionCode(context: Context): Long? {
            return try {
                val info = context.packageManager.getPackageInfo(BAZAAR_PACKAGE, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
                else @Suppress("DEPRECATION") info.versionCode.toLong()
            } catch (_: Exception) {
                null
            }
        }

        /** Check if Bazaar supports the required version */
        fun isVersionSupported(context: Context, requiredVersion: Int): Boolean {
            val current = getInstalledVersionCode(context) ?: return false
            return current >= requiredVersion
        }
    }

    // ===============================================================
    // 📋 Core Diagnostic & Info Utilities
    // ===============================================================

    fun describe(): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        return """
            ⚠️ BazaarNotSupportedException
            ──────────────────────────────
            Time        : $date
            Message     : ${message ?: "Unknown"}
            Package     : $packageName
            Required Ver: ${requiredVersion ?: "Unknown"}
            Current Ver : ${currentVersion ?: "Unknown"}
            Hint        : ${recoveryHint ?: "Please update Bazaar manually"}
        """.trimIndent()
    }

    fun log(tag: String = TAG) {
        Log.e(tag, "❌ BazaarNotSupportedException: $message")
        Log.e(tag, "• Package: $packageName")
        Log.e(tag, "• Required version: ${requiredVersion ?: "?"}")
        Log.e(tag, "• Current version : ${currentVersion ?: "?"}")
        Log.e(tag, "• Hint : $recoveryHint")
    }

    fun toJson(pretty: Boolean = true): String {
        val obj = JSONObject().apply {
            put("error", "BazaarNotSupportedException")
            put("message", message)
            put("timestamp", timestamp)
            put("package", packageName)
            put("requiredVersion", requiredVersion)
            put("currentVersion", currentVersion)
            put("recoveryHint", recoveryHint)
        }
        return if (pretty) obj.toString(2) else obj.toString()
    }

    fun getRecoverySuggestion(): String =
        recoveryHint ?: "Please update Bazaar to the latest version."

    // ===============================================================
    // 🎨 Modern, Beautiful UI Helpers
    // ===============================================================

    fun showAsToast(context: Context, duration: Int = Toast.LENGTH_LONG) {
        val msg = getRecoverySuggestion()
        val runnable = Runnable {
            Toast.makeText(context.applicationContext, msg, duration).show()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) runnable.run()
        else Handler(Looper.getMainLooper()).post(runnable)
    }

    /**
     * A stylish, user-friendly update dialog with better typography and color scheme.
     */
    fun showUpdateDialog(
        context: Context,
        onUpdate: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        val showDialog = {
            try {
                val titleView = TextView(context).apply {
                    text = "⚠️ Bazaar Update Required"
                    textSize = 20f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                    setPadding(50, 40, 50, 20)
                }

                val messageView = TextView(context).apply {
                    text = """
                        Your version of Bazaar is outdated. 
                        Please update to continue using all features.

                        ${getRecoverySuggestion()}
                    """.trimIndent()
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_dark))
                    setPadding(50, 0, 50, 20)
                }

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(titleView)
                    addView(messageView)
                }

                val builder = AlertDialog.Builder(context)
                    .setView(layout)
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
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_green_light)
                    )
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_red_light)
                    )
                }

                dialog.show()
            } catch (e: Exception) {
                Log.w(TAG, "Dialog failed: ${e.message}")
                showAsToast(context)
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) showDialog()
        else Handler(Looper.getMainLooper()).post(showDialog)
    }

    fun openBazaarUpdatePage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, getUpdateUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Fallback to web
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, getWebUpdateUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open web update page: ${e.message}")
                showAsToast(context)
            }
        }
    }

    // ===============================================================
    // 🧩 Smart Logic & Helper Tools
    // ===============================================================

    fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun needsUpdate(): Boolean =
        requiredVersion != null && currentVersion != null && currentVersion < requiredVersion

    fun elapsedSinceThrown(): Long =
        (System.currentTimeMillis() - timestamp) / 1000

    fun getReadableElapsedTime(): String {
        val seconds = elapsedSinceThrown()
        val minutes = seconds / 60
        val remaining = seconds % 60
        return if (minutes > 0) "${minutes}m ${remaining}s ago" else "${remaining}s ago"
    }

    fun getLocalizedRecoveryHint(locale: Locale = Locale.getDefault()): String =
        when (locale.language.lowercase(Locale.getDefault())) {
            "fa" -> "لطفاً بازار را به‌روزرسانی کنید تا بتوانید ادامه دهید."
            else -> recoveryHint ?: "Please update Bazaar to continue."
        }

    fun smartRecoveryStrategy(context: Context): String = when {
        !isBazaarInstalled(context) -> "Bazaar not installed. Please install it first."
        !hasInternetConnection(context) -> "No internet connection. Please connect and try again."
        needsUpdate() -> "Your Bazaar version is outdated. Please update now."
        else -> "Unknown issue. Try restarting your device or reinstalling Bazaar."
    }

    fun toAnalyticsBundle(): Map<String, String> = mapOf(
        "exception" to "BazaarNotSupportedException",
        "package" to packageName,
        "required_version" to (requiredVersion?.toString() ?: "unknown"),
        "current_version" to (currentVersion?.toString() ?: "unknown"),
        "elapsed_seconds" to elapsedSinceThrown().toString(),
        "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "android_version" to Build.VERSION.RELEASE
    )

    fun compactSummary(): String =
        "[$packageName] Bazaar outdated (req=$requiredVersion, cur=$currentVersion)"

    fun getErrorId(): String = UUID.randomUUID().toString().substring(0, 8)

    fun diagnosticReport(): String = buildString {
        appendLine(toDebugString())
        appendLine(describe())
        appendLine("Elapsed: ${getReadableElapsedTime()}")
    }

    fun toDebugString(): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        return "[BazaarNotSupported] ($time) ${message ?: "No message"}"
    }

    override fun toString(): String =
        "BazaarNotSupportedException(packageName=$packageName, requiredVersion=$requiredVersion, currentVersion=$currentVersion, message=$message, timestamp=$timestamp)"
}
