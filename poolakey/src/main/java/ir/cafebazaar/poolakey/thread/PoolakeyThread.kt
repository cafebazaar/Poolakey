// =======================================================
// ✅ Poolakey Complete Utilities + Extensions (Fixed + Enhanced)
// =======================================================

package ir.cafebazaar.poolakey

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

// =======================================================
// 📦 Package Utilities
// =======================================================

internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    context.packageManager.getPackageInfo(packageName, 0)
} catch (_: Exception) { null }

@Suppress("DEPRECATION")
internal fun sdkAwareVersionCode(info: PackageInfo): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

internal fun getVersionName(context: Context, pkg: String): String? =
    getPackageInfo(context, pkg)?.versionName

internal fun getFirstInstallTime(context: Context, pkg: String): Long? =
    getPackageInfo(context, pkg)?.firstInstallTime

internal fun getLastUpdateTime(context: Context, pkg: String): Long? =
    getPackageInfo(context, pkg)?.lastUpdateTime

internal fun getAppAgeInDays(context: Context, pkg: String): Long? {
    val installTime = getFirstInstallTime(context, pkg) ?: return null
    return (System.currentTimeMillis() - installTime) / (1000L * 60 * 60 * 24)
}

internal fun getInstallDate(context: Context, pkg: String): String {
    val install = getFirstInstallTime(context, pkg) ?: return "Unknown"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(install))
}

internal fun getUpdateDate(context: Context, pkg: String): String {
    val update = getLastUpdateTime(context, pkg) ?: return "Unknown"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(update))
}

internal fun wasRecentlyUpdated(context: Context, pkg: String, days: Int = 7): Boolean {
    val last = getLastUpdateTime(context, pkg) ?: return false
    val since = (System.currentTimeMillis() - last) / (1000 * 60 * 60 * 24)
    return since <= days
}

// =======================================================
// 🔌 Connection State Management
// =======================================================

sealed class ConnectionState {
    object Connected : ConnectionState()
    object FailedToConnect : ConnectionState()
    object Disconnected : ConnectionState()

    fun isConnected() = this is Connected
    fun isDisconnected() = this is Disconnected
    fun isFailed() = this is FailedToConnect

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
        Log.d(tag, "State: ${toStatusCode()} (${getEmoji()})")
    }

    @ColorInt
    fun getStateColor(): Int = when (this) {
        Connected -> Color.parseColor("#4CAF50")
        FailedToConnect -> Color.parseColor("#F44336")
        Disconnected -> Color.parseColor("#FFC107")
    }

    fun getUiLabel(): String = "${getEmoji()} ${toStatusCode()}"

    /** improved, gradient-based badge with shadow */
    fun bindToStatusView(view: TextView, ctx: Context) {
        val grad = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                adjustBrightness(getStateColor(), 1.2f),
                adjustBrightness(getStateColor(), 0.8f)
            )
        ).apply {
            cornerRadius = dp(ctx, 24f)
        }

        view.apply {
            text = getUiLabel()
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 15f
            setTextColor(Color.WHITE)
            background = grad
            setPadding(dp(ctx, 20f).toInt(), dp(ctx, 10f).toInt(), dp(ctx, 20f).toInt(), dp(ctx, 10f).toInt())
            elevation = 8f
        }
    }

    private fun dp(ctx: Context, v: Float): Float = v * ctx.resources.displayMetrics.density

    private fun adjustBrightness(@ColorInt color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).coerceIn(0f, 255f)
        val g = (Color.green(color) * factor).coerceIn(0f, 255f)
        val b = (Color.blue(color) * factor).coerceIn(0f, 255f)
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }
}

// =======================================================
// 🔗 Connection Interface + Extensions
// =======================================================

interface Connection {
    fun getState(): ConnectionState
    fun disconnect()
}

class SafeConnection(private val ref: WeakReference<Connection>) : Connection {
    override fun getState(): ConnectionState = ref.get()?.getState() ?: ConnectionState.Disconnected
    override fun disconnect() { ref.get()?.disconnect() }

    fun reconnectIfNeeded(onReconnect: () -> Unit) {
        if (getState().isDisconnected() || getState().isFailed()) {
            Log.i("Poolakey", "Reconnecting …")
            onReconnect()
        }
    }
}

fun ConnectionState.asLiveData(): LiveData<ConnectionState> = MutableLiveData(this)
fun ConnectionState.asStateFlow(): StateFlow<ConnectionState> = MutableStateFlow(this)

fun combineConnectionStates(vararg s: ConnectionState): ConnectionState =
    when {
        s.any { it is ConnectionState.FailedToConnect } -> ConnectionState.FailedToConnect
        s.any { it is ConnectionState.Disconnected } -> ConnectionState.Disconnected
        else -> ConnectionState.Connected
    }

// =======================================================
// 🧵 Poolakey Thread Interface + Implementations
// =======================================================

internal interface PoolakeyThread<T> {
    fun execute(task: T)
    fun dispose()
}

internal class CoroutinePoolakeyThread : PoolakeyThread<Runnable> {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    override fun execute(task: Runnable) { scope.launch { task.run() } }
    override fun dispose() { job.cancel() }
}

internal class MainThreadPoolakeyThread : PoolakeyThread<() -> Unit> {
    private val handler = Handler(Looper.getMainLooper())
    override fun execute(task: () -> Unit) { handler.post(task) }
    override fun dispose() { handler.removeCallbacksAndMessages(null) }
}

// =======================================================
// 🧠 Connection Monitoring Extensions
// =======================================================

suspend fun ConnectionState.monitorConnectionState(
    onConnected: suspend () -> Unit,
    onDisconnected: suspend () -> Unit,
    onFailed: suspend () -> Unit
) {
    when (this) {
        is ConnectionState.Connected -> onConnected()
        is ConnectionState.Disconnected -> onDisconnected()
        is ConnectionState.FailedToConnect -> onFailed()
    }
}

suspend fun retryUntilConnected(
    provider: suspend () -> ConnectionState,
    maxRetries: Int = 5,
    delayMs: Long = 2000L
): ConnectionState {
    repeat(maxRetries) { attempt ->
        val state = provider()
        if (state.isConnected()) return state
        Log.w("PoolakeyRetry", "Attempt ${attempt + 1}/$maxRetries failed – retrying …")
        delay(delayMs)
    }
    return ConnectionState.FailedToConnect
}
