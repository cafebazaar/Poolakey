package ir.cafebazaar.poolakey

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts

internal class PaymentLauncher private constructor(
    val activityLauncher: ActivityResultLauncher<Intent>,
    val intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

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
    }
}