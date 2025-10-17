package ir.cafebazaar.poolakey

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// -------------------- Package Utilities --------------------

internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    val packageManager = context.packageManager
    packageManager.getPackageInfo(packageName, 0)
} catch (ignored: Exception) {
    null
}

@Suppress("DEPRECATION")
internal fun sdkAwareVersionCode(packageInfo: PackageInfo): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode
    else packageInfo.versionCode.toLong()

internal fun getVersionName(context: Context, packageName: String): String? =
    getPackageInfo(context, packageName)?.versionName

internal fun isPackageInstalled(context: Context, packageName: String): Boolean =
    try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }

internal fun isVersionAtLeast(context: Context, packageName: String, minVersionCode: Long): Boolean {
    val info = getPackageInfo(context, packageName)
    return info?.let { sdkAwareVersionCode(it) >= minVersionCode } ?: false
}

internal fun logPackageInfo(context: Context, packageName: String) {
    val info = getPackageInfo(context, packageName) ?: run {
        Log.w("Poolakey", "⚠️ Package $packageName not found or inaccessible.")
        return
    }
    val versionName = info.versionName ?: "N/A"
    val versionCode = sdkAwareVersionCode(info)
    Log.d("Poolakey", """
        📦 Package Info:
        - Package Name: $packageName
        - Version Name: $versionName
        - Version Code: $versionCode
        - First Install Time: ${info.firstInstallTime}
        - Last Update Time: ${info.lastUpdateTime}
    """.trimIndent())
}

internal fun getFirstInstallTime(context: Context, packageName: String): Long? =
    getPackageInfo(context, packageName)?.firstInstallTime

internal fun getLastUpdateTime(context: Context, packageName: String): Long? =
    getPackageInfo(context, packageName)?.lastUpdateTime

internal fun compareAppVersions(context: Context, packageName1: String, packageName2: String): Int {
    val info1 = getPackageInfo(context, packageName1)
    val info2 = getPackageInfo(context, packageName2)
    if (info1 == null || info2 == null) return 0
    return sdkAwareVersionCode(info1).compareTo(sdkAwareVersionCode(info2))
}

internal fun getPackageSummary(context: Context, packageName: String): String {
    val info = getPackageInfo(context, packageName)
    return if (info == null) "Package \"$packageName\" is not installed."
    else """
        📦 $packageName
        • Version: ${info.versionName ?: "Unknown"} (${sdkAwareVersionCode(info)})
        • Installed: ${info.firstInstallTime}
        • Updated: ${info.lastUpdateTime}
    """.trimIndent()
}

internal fun getAppAgeInDays(context: Context, packageName: String): Long? {
    val installTime = getFirstInstallTime(context, packageName) ?: return null
    return (System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)
}

internal fun needsUpdate(context: Context, packageName: String, remoteVersionCode: Long): Boolean {
    val currentVersion = getPackageInfo(context, packageName)?.let { sdkAwareVersionCode(it) } ?: return false
    return currentVersion < remoteVersionCode
}

internal fun getPackageUiSummary(context: Context, packageName: String): String {
    val info = getPackageInfo(context, packageName) ?: return "Package \"$packageName\" not installed."
    val age = getAppAgeInDays(context, packageName) ?: 0
    return "${info.packageName} v${info.versionName ?: "?"} (${sdkAwareVersionCode(info)}) - Installed $age days ago"
}

// -------------------- Connection State --------------------

sealed class ConnectionState {

    object Connected : ConnectionState()
    object FailedToConnect : ConnectionState()
    object Disconnected : ConnectionState()

    fun isConnected(): Boolean = this is Connected
    fun isDisconnected(): Boolean = this is Disconnected
    fun isFailed(): Boolean = this is FailedToConnect

    fun getDescription(): String = when (this) {
        Connected -> "✅ Connected to Bazaar billing service."
        FailedToConnect -> "❌ Failed to connect to Bazaar billing service."
        Disconnected -> "⚠️ Disconnected from Bazaar billing service."
    }

    fun toStatusCode(): String = when (this) {
        Connected -> "CONNECTED"
        FailedToConnect -> "FAILED"
        Disconnected -> "DISCONNECTED"
    }

    fun getEmoji(): String = when (this) {
        Connected -> "🟢"
        FailedToConnect -> "🔴"
        Disconnected -> "🟡"
    }

    fun logState(tag: String = "PoolakeyConnection") {
        Log.d(tag, "Connection State: ${toStatusCode()} - ${getDescription()}")
    }

    @ColorInt
    fun getStateColor(): Int = when (this) {
        Connected -> Color.parseColor("#4CAF50")
        FailedToConnect -> Color.parseColor("#F44336")
        Disconnected -> Color.parseColor("#FFC107")
    }

    fun getUiLabel(): String = "${getEmoji()} ${toStatusCode()}"

    fun bindToStatusView(textView: TextView, context: Context) {
        textView.text = getUiLabel()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(context, 16).toFloat()
            setColor(getStateColor())
        }
        textView.apply {
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 14f
            setPadding(dpToPx(context, 12), dpToPx(context, 6), dpToPx(context, 12), dpToPx(context, 6))
            background = drawable
        }
    }

    fun styleTextViewAsBadge(textView: TextView, context: Context) {
        val color = getStateColor()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(context, 12).toFloat()
            setColor(adjustAlpha(color, 0.15f))
        }
        textView.apply {
            text = getUiLabel()
            setTextColor(color)
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 13f
            setPadding(dpToPx(context, 10), dpToPx(context, 4), dpToPx(context, 10), dpToPx(context, 4))
            background = drawable
        }
    }

    fun shouldAttemptReconnect(): Boolean = !isConnected()
    fun getNextSuggestedState(): ConnectionState = when (this) {
        Connected -> Disconnected
        else -> Connected
    }

    fun toNotificationMessage(): String = when (this) {
        Connected -> "🟢 Connection established with Bazaar."
        FailedToConnect -> "🔴 Unable to reach Bazaar servers. Please try again."
        Disconnected -> "🟡 Connection lost. Attempting to reconnect..."
    }

    fun getConnectionStabilityScore(): Int = when (this) {
        Connected -> 100
        FailedToConnect -> 25
        Disconnected -> 50
    }

    fun asLiveData(): LiveData<ConnectionState> = MutableLiveData<ConnectionState>().apply { value = this@ConnectionState }
    fun asStateFlow(): StateFlow<ConnectionState> = MutableStateFlow(this)

    private fun dpToPx(context: Context, dp: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

// -------------------- Connection Interface --------------------

interface Connection {
    fun getState(): ConnectionState
    fun disconnect()
}

// -------------------- Additional ConnectionState Helpers --------------------

fun ConnectionState.toggleTestState(): ConnectionState = when (this) {
    ConnectionState.Connected -> ConnectionState.Disconnected
    ConnectionState.Disconnected -> ConnectionState.Connected
    ConnectionState.FailedToConnect -> ConnectionState.Connected
}

fun ConnectionState.getSeverityLevel(): Int = when (this) {
    ConnectionState.Connected -> 0
    ConnectionState.Disconnected -> 1
    ConnectionState.FailedToConnect -> 2
}

fun ConnectionState.updateTextViews(vararg textViews: TextView, context: Context) {
    textViews.forEach { bindToStatusView(it, context) }
}

fun ConnectionState.getUiLabelWithMessage(message: String?): String =
    if (message.isNullOrBlank()) getUiLabel() else "${getUiLabel()} - $message"

fun ConnectionState.getColoredCircleDrawable(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(getStateColor())
    setSize(24, 24)
}

fun ConnectionState.asConnectedLiveData(): LiveData<Boolean> = MutableLiveData<Boolean>().apply { value = isConnected() }
fun ConnectionState.asStabilityStateFlow(): StateFlow<Int> = MutableStateFlow(getConnectionStabilityScore()) 
