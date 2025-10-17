// =======================================================
// ✅ Poolakey Complete Utilities + Thread Implementations (Enhanced Edition)
// =======================================================

package ir.cafebazaar.poolakey

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

// =======================================================
// 📦 Package Utilities
// =======================================================

internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    context.packageManager.getPackageInfo(packageName, 0)
} catch (_: Exception) {
    null
}

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

    /**
     * 🌈 Visually improved UI for connection status
     */
    @MainThread
    fun bindToStatusView(view: TextView, ctx: Context) {
        val color = getStateColor()
        val startGradient = adjustBrightness(color, 1.25f)
        val endGradient = adjustBrightness(color, 0.85f)

        val grad = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startGradient, endGradient)
        ).apply {
            cornerRadius = dp(ctx, 24f)
        }

        // Animate color transition for smoother feedback
        val prevBackground = (view.background as? GradientDrawable)?.colors?.firstOrNull() ?: color
        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), prevBackground, color).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                view.setBackgroundColor(it.animatedValue as Int)
            }
        }

        colorAnim.start()

        view.apply {
            text = getUiLabel()
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 15f
            setTextColor(Color.WHITE)
            background = grad
            setPadding(dp(ctx, 20f).toInt(), dp(ctx, 10f).toInt(), dp(ctx, 20f).toInt(), dp(ctx, 10f).toInt())
            elevation = 8f
            alpha = 0f
            animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun dp(ctx: Context, v: Float): Float = v * ctx.resources.displayMetrics.density

    private fun adjustBrightness(@ColorInt color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).coerceIn(0f, 255f)
        val g = (Color.green(color) * factor).coerceIn(0f, 255f)
        val b = (Color.blue(color) * factor).coerceIn(0f, 255f)
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    fun formatMessage(): String {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        return "[$ts] ${getUiLabel()}"
    }

    fun toIntCode(): Int = when (this) {
        Connected -> 1
        FailedToConnect -> -1
        Disconnected -> 0
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
    override fun disconnect() {
        ref.get()?.disconnect()
    }

    fun reconnectIfNeeded(onReconnect: () -> Unit) {
        if (getState().isDisconnected() || getState().isFailed()) {
            Log.i("Poolakey", "Reconnecting …")
            onReconnect()
        }
    }
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
    override fun execute(task: Runnable) {
        scope.launch { task.run() }
    }

    override fun dispose() {
        job.cancel()
    }
}

internal class MainThread : Handler(Looper.getMainLooper()), PoolakeyThread<() -> Unit> {
    override fun handleMessage(message: Message) {
        super.handleMessage(message)
        (message.obj as? Function0<*>)?.invoke()
            ?: Log.e("MainThread", "Message is corrupted!")
    }

    override fun execute(task: () -> Unit) {
        val msg = Message.obtain().apply { obj = task }
        sendMessage(msg)
    }

    override fun dispose() {
        removeCallbacksAndMessages(null)
    }
}

internal class BackgroundThread : HandlerThread("PoolakeyThread"), PoolakeyThread<Runnable> {
    private lateinit var threadHandler: Handler

    init {
        start()
        threadHandler = Handler(looper)
    }

    override fun execute(task: Runnable) {
        if (::threadHandler.isInitialized) {
            threadHandler.post(task)
        }
    }

    override fun dispose() {
        threadHandler.removeCallbacksAndMessages(null)
        quitSafely()
    }
}

// =======================================================
// 🧠 Advanced Utilities & UI Diagnostics
// =======================================================

fun ConnectionState.renderTo(view: TextView) {
    val formatted = "${getEmoji()} ${toStatusCode()} • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    view.text = formatted
    view.setTextColor(getStateColor())
    view.animate().alpha(1f).setDuration(250).start()
}

fun ConnectionState.toJsonString(): String =
    """{"status":"${toStatusCode()}","emoji":"${getEmoji()}","code":${toIntCode()},"timestamp":${System.currentTimeMillis()}}"""

fun collectConnectionDiagnostics(connection: Connection): Map<String, Any> = mapOf(
    "state" to connection.getState().toStatusCode(),
    "connected" to connection.getState().isConnected(),
    "device" to Build.MODEL,
    "sdk" to Build.VERSION.SDK_INT,
    "time" to System.currentTimeMillis()
)
