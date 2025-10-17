package ir.cafebazaar.poolakey

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the current connection state between the app and Bazaar billing service.
 * Extended with useful helpers for logging, UI binding and reactive adapters.
 */
sealed class ConnectionState {

    /** Indicates that a connection has been successfully established. */
    object Connected : ConnectionState()

    /** Indicates that the connection attempt has failed. */
    object FailedToConnect : ConnectionState()

    /** Indicates that the service has been disconnected. */
    object Disconnected : ConnectionState()

    // -------------------- Basic queries --------------------

    /** Returns `true` if the current state represents an active connection. */
    fun isConnected(): Boolean = this is Connected

    /** Returns `true` if the current state represents a disconnection. */
    fun isDisconnected(): Boolean = this is Disconnected

    /** Returns `true` if the current state represents a failed connection attempt. */
    fun isFailed(): Boolean = this is FailedToConnect

    /** Provides a human-readable description of the current connection state. */
    fun getDescription(): String = when (this) {
        Connected -> "✅ Connected to Bazaar billing service."
        FailedToConnect -> "❌ Failed to connect to Bazaar billing service."
        Disconnected -> "⚠️ Disconnected from Bazaar billing service."
    }

    /** Converts the connection state to a short status code string. */
    fun toStatusCode(): String = when (this) {
        Connected -> "CONNECTED"
        FailedToConnect -> "FAILED"
        Disconnected -> "DISCONNECTED"
    }

    /** Returns a simple emoji-based representation for logs or light UIs. */
    fun getEmoji(): String = when (this) {
        Connected -> "🟢"
        FailedToConnect -> "🔴"
        Disconnected -> "🟡"
    }

    // -------------------- Logging & diagnostics --------------------

    /** Prints a formatted debug log describing the current state. */
    fun logState(tag: String = "PoolakeyConnection") {
        android.util.Log.d(tag, "Connection State: ${toStatusCode()} - ${getDescription()}")
    }

    /** Returns a compact JSON-like representation (for logs or diagnostics). */
    fun toJsonString(): String {
        return """{
            "state": "${toStatusCode()}",
            "description": "${getDescription()}",
            "emoji": "${getEmoji()}",
            "reconnectAllowed": ${shouldAttemptReconnect()},
            "stability": ${getConnectionStabilityScore()}
        }""".trimIndent()
    }

    // -------------------- UI helpers --------------------

    /**
     * Maps the current state to a color code for UI display.
     * Uses Color.parseColor for clear, portable values.
     */
    @ColorInt
    fun getStateColor(): Int = when (this) {
        Connected -> Color.parseColor("#4CAF50")   // Green
        FailedToConnect -> Color.parseColor("#F44336") // Red
        Disconnected -> Color.parseColor("#FFC107") // Amber
    }

    /** Builds a short formatted UI-friendly string. Example: "🟢 Connected (Active)" */
    fun getUiLabel(): String = when (this) {
        Connected -> "${getEmoji()} Connected (Active)"
        FailedToConnect -> "${getEmoji()} Connection Failed"
        Disconnected -> "${getEmoji()} Disconnected"
    }

    /**
     * Styles a TextView to represent the connection state as a pill/badge.
     *
     * Example usage (on main thread):
     *   connectionState.bindToStatusView(statusTextView, context)
     */
    fun bindToStatusView(textView: TextView, context: Context) {
        // text
        textView.text = getUiLabel()

        // color
        val color = getStateColor()
        textView.setTextColor(Color.WHITE)

        // typeface and size
        textView.setTypeface(Typeface.DEFAULT_BOLD)
        textView.textSize = 14f

        // padding (left, top, right, bottom)
        val horizontal = dpToPx(context, 12)
        val vertical = dpToPx(context, 6)
        textView.setPadding(horizontal, vertical, horizontal, vertical)

        // rounded background (pill) with appropriate color
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = dpToPx(context, 20).toFloat()
        // use semi-strong color for background; keep text white for contrast
        drawable.setColor(color)
        textView.background = drawable
    }

    /**
     * Applies a lighter badge-style background and colored text (inverse of bindToStatusView).
     * Useful when you want colored text on transparent/light background.
     */
    fun styleTextViewAsBadge(textView: TextView, context: Context) {
        val color = getStateColor()
        textView.text = getUiLabel()
        textView.setTextColor(color)
        textView.setTypeface(Typeface.DEFAULT_BOLD)
        textView.textSize = 13f
        val horizontal = dpToPx(context, 10)
        val vertical = dpToPx(context, 4)
        textView.setPadding(horizontal, vertical, horizontal, vertical)

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = dpToPx(context, 16).toFloat()
        // light translucent background
        drawable.setColor(adjustAlpha(color, 0.12f))
        textView.background = drawable
    }

    // -------------------- Connection logic helpers --------------------

    /**
     * Suggests whether the system should attempt to reconnect.
     * Returns true if the current state is FailedToConnect or Disconnected.
     */
    fun shouldAttemptReconnect(): Boolean = when (this) {
        Connected -> false
        FailedToConnect, Disconnected -> true
    }

    /**
     * Returns a next recommended state based on current state and external conditions.
     */
    fun getNextSuggestedState(): ConnectionState = when (this) {
        Connected -> Disconnected
        FailedToConnect, Disconnected -> Connected
    }

    /**
     * Returns a notification-friendly message for the current state.
     */
    fun toNotificationMessage(): String = when (this) {
        Connected -> "🟢 Connection established with Bazaar."
        FailedToConnect -> "🔴 Unable to reach Bazaar servers. Please try again."
        Disconnected -> "🟡 Connection lost. Attempting to reconnect..."
    }

    /**
     * Returns an estimated connection stability score (0–100).
     */
    fun getConnectionStabilityScore(): Int = when (this) {
        Connected -> 100
        FailedToConnect -> 25
        Disconnected -> 50
    }

    // -------------------- Reactive adapters --------------------

    /**
     * Converts the state into a LiveData value — helpful for UI observation.
     * Note: returned LiveData contains the single value of this state.
     */
    fun asLiveData(): LiveData<ConnectionState> {
        val liveData = MutableLiveData<ConnectionState>()
        liveData.value = this
        return liveData
    }

    /**
     * Converts the state into a StateFlow value — useful for reactive UIs.
     * Note: returned StateFlow contains this state as its initial value.
     */
    fun asStateFlow(): StateFlow<ConnectionState> {
        return MutableStateFlow(this)
    }

    // -------------------- Utility helpers --------------------

    private fun dpToPx(context: Context, dp: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }
}
