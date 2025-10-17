package ir.cafebazaar.poolakey

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Package / version helper utilities for Poolakey.
 *
 * - Safe retrieval of PackageInfo
 * - SDK-aware version code reading
 * - Convenience checks and nicely formatted outputs
 * - A small native dialog helper to display package info in a visually friendly way
 */

/**
 * Safely retrieves [PackageInfo] for the given package name.
 */
internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    val packageManager = context.packageManager
    packageManager.getPackageInfo(packageName, 0)
} catch (ignored: Exception) {
    null
}

/**
 * Returns the SDK-aware version code from the [PackageInfo].
 */
@Suppress("DEPRECATION")
internal fun sdkAwareVersionCode(packageInfo: PackageInfo): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
}

// -------------------- Additional Utility Functions --------------------

/**
 * Retrieves the version name of the given package, or null if unavailable.
 */
internal fun getVersionName(context: Context, packageName: String): String? {
    return getPackageInfo(context, packageName)?.versionName
}

/**
 * Checks whether a given package is installed on the device.
 */
internal fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    } catch (e: Exception) {
        false
    }
}

/**
 * Returns true if the given package's version is greater than or equal to [minVersionCode].
 */
internal fun isVersionAtLeast(context: Context, packageName: String, minVersionCode: Long): Boolean {
    val info = getPackageInfo(context, packageName)
    val currentCode = info?.let { sdkAwareVersionCode(it) } ?: return false
    return currentCode >= minVersionCode
}

/**
 * Logs detailed package info for debugging purposes (formatted).
 */
internal fun logPackageInfo(context: Context, packageName: String) {
    val info = getPackageInfo(context, packageName)
    if (info == null) {
        Log.w("Poolakey", "⚠️ Package $packageName not found or inaccessible.")
        return
    }
    val versionName = info.versionName ?: "N/A"
    val versionCode = sdkAwareVersionCode(info)
    val first = formatTime(info.firstInstallTime)
    val last = formatTime(info.lastUpdateTime)

    val message = """
        📦 Package Info:
        • Package Name: $packageName
        • Version Name: $versionName
        • Version Code: $versionCode
        • First Install: $first
        • Last Update : $last
    """.trimIndent()

    Log.d("Poolakey", message)
}

/**
 * Returns the first install time of the app in milliseconds.
 */
internal fun getFirstInstallTime(context: Context, packageName: String): Long? {
    return getPackageInfo(context, packageName)?.firstInstallTime
}

/**
 * Returns the last update time of the app in milliseconds.
 */
internal fun getLastUpdateTime(context: Context, packageName: String): Long? {
    return getPackageInfo(context, packageName)?.lastUpdateTime
}

/**
 * Compares two installed app versions by version code.
 * @return positive if [packageName1] > [packageName2], negative if less, 0 if equal or unknown.
 */
internal fun compareAppVersions(context: Context, packageName1: String, packageName2: String): Int {
    val info1 = getPackageInfo(context, packageName1)
    val info2 = getPackageInfo(context, packageName2)
    if (info1 == null || info2 == null) return 0
    val code1 = sdkAwareVersionCode(info1)
    val code2 = sdkAwareVersionCode(info2)
    return code1.compareTo(code2)
}

/**
 * Returns human-readable package info summary (for UI or logs).
 */
internal fun getPackageSummary(context: Context, packageName: String): String {
    val info = getPackageInfo(context, packageName)
    return if (info == null) {
        "Package \"$packageName\" is not installed."
    } else {
        val versionName = info.versionName ?: "Unknown"
        val versionCode = sdkAwareVersionCode(info)
        val installTime = formatTime(info.firstInstallTime)
        val updateTime = formatTime(info.lastUpdateTime)
        """
        📦 $packageName
        • Version: $versionName ($versionCode)
        • Installed: $installTime
        • Updated:   $updateTime
        """.trimIndent()
    }
}

/**
 * Returns a short, visually-appealing summary (single-line) useful for compact UIs or logs.
 */
internal fun getPackageShortSummary(context: Context, packageName: String): String {
    val info = getPackageInfo(context, packageName)
    return if (info == null) {
        "⚠️ $packageName — not installed"
    } else {
        val versionName = info.versionName ?: "?"
        val versionCode = sdkAwareVersionCode(info)
        "📦 $packageName — $versionName ($versionCode)"
    }
}

/**
 * Shows a simple native dialog with pretty package information.
 * Uses android.app.AlertDialog so it works with plain Context (but passing an Activity Context
 * is recommended so dialog is themed correctly).
 */
internal fun showPackageInfoDialog(context: Context, packageName: String) {
    val info = getPackageInfo(context, packageName)
    val title = "Package Info"
    val message = if (info == null) {
        "Package \"$packageName\" is not installed on this device."
    } else {
        val versionName = info.versionName ?: "Unknown"
        val versionCode = sdkAwareVersionCode(info)
        val installed = formatTime(info.firstInstallTime)
        val updated = formatTime(info.lastUpdateTime)
        """
        📦 $packageName

        • Version: $versionName
        • Version Code: $versionCode

        • Installed: $installed
        • Updated:   $updated
        """.trimIndent()
    }

    // Build and show the dialog (safe: will use application context fallback).
    try {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    } catch (e: Exception) {
        // If showing a dialog fails (e.g., non-activity context), fall back to logging.
        Log.w("Poolakey", "Unable to show dialog for $packageName: ${e.message}")
        Log.d("Poolakey", message)
    }
}

// -------------------- Helpers --------------------

private fun formatTime(epochMillis: Long): String {
    return try {
        if (epochMillis <= 0L) "N/A"
        else {
            val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            df.format(Date(epochMillis))
        }
    } catch (e: Exception) {
        epochMillis.toString()
    }
}
