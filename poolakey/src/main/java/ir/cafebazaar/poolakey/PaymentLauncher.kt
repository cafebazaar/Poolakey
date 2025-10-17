package ir.cafebazaar.poolakey

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts

internal class PaymentLauncher private constructor(
    val activityLauncher: ActivityResultLauncher<Intent>,
    val intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    companion object {
        private const val TAG = "PaymentLauncher"

        // ANSI color codes for terminal (logs)
        private const val RESET = "\u001B[0m"
        private const val GREEN = "\u001B[32m"
        private const val RED = "\u001B[31m"
        private const val YELLOW = "\u001B[33m"
        private const val CYAN = "\u001B[36m"
        private const val BOLD = "\u001B[1m"
    }

    class Builder(
        private val registry: ActivityResultRegistry,
        private val onActivityResult: (ActivityResult) -> Unit
    ) {

        fun build(): PaymentLauncher {
            val activityLauncher = registry.register(
                BillingConnection.PAYMENT_SERVICE_KEY,
                ActivityResultContracts.StartActivityForResult(),
                onActivityResult::invoke
            )

            val intentSenderLauncher = registry.register(
                BillingConnection.PAYMENT_SERVICE_KEY,
                ActivityResultContracts.StartIntentSenderForResult(),
                onActivityResult::invoke
            )

            return PaymentLauncher(activityLauncher, intentSenderLauncher)
        }
    }

    fun unregister() {
        activityLauncher.unregister()
        intentSenderLauncher.unregister()
        logInfo("Launchers unregistered")
    }

    // -------------------- Additional Helper Functions --------------------

    /** Launch a payment via Intent */
    fun launchPayment(intent: Intent) {
        try {
            activityLauncher.launch(intent)
            logSuccess("Payment launched via Intent")
        } catch (e: Exception) {
            logError("Failed to launch payment Intent: ${e.message}")
        }
    }

    /** Launch a payment via IntentSenderRequest */
    fun launchPayment(intentSenderRequest: IntentSenderRequest) {
        try {
            intentSenderLauncher.launch(intentSenderRequest)
            logSuccess("Payment launched via IntentSenderRequest")
        } catch (e: Exception) {
            logError("Failed to launch payment IntentSenderRequest: ${e.message}")
        }
    }

    /** Check if launchers are currently registered */
    fun areLaunchersRegistered(): Boolean {
        return try {
            activityLauncher.isEnabled && intentSenderLauncher.isEnabled
        } catch (e: Exception) {
            false
        }
    }

    /** Safely relaunch payment if launchers were unregistered */
    fun relaunchPayment(
        registry: ActivityResultRegistry,
        onActivityResult: (ActivityResult) -> Unit,
        intent: Intent? = null,
        intentSenderRequest: IntentSenderRequest? = null
    ) {
        logInfo("Relaunching payment...")
        val builder = Builder(registry, onActivityResult)
        val newLauncher = builder.build()
        intent?.let { newLauncher.launchPayment(it) }
        intentSenderRequest?.let { newLauncher.launchPayment(it) }
    }

    /** Launch payment and log results for debugging */
    fun launchPaymentWithLogging(intent: Intent) {
        logInfo("Attempting to launch payment...")
        launchPayment(intent)
        logInfo("Payment launch request complete")
    }

    // -------------------- Logging Helpers --------------------
    private fun logError(message: String) {
        Log.e(TAG, "$RED$BOLD❌ ERROR: $message$RESET")
    }

    private fun logSuccess(message: String) {
        Log.d(TAG, "$GREEN$BOLD✅ SUCCESS: $message$RESET")
    }

    private fun logInfo(message: String) {
        Log.i(TAG, "$CYANℹ️ INFO: $message$RESET")
    }
}
