package ir.cafebazaar.poolakey.billing

import android.os.Bundle

internal object FeatureConfig {

    private const val INTENT_TRIAL_SUBSCRIPTION_SUPPORT = "INTENT_TRIAL_SUBSCRIPTION_SUPPORT"

    internal fun isCheckTrialSubscriptionAvailable(featureConfigBundle: Bundle?): Boolean {
        return featureConfigBundle?.getBoolean(
            INTENT_TRIAL_SUBSCRIPTION_SUPPORT,
            false
        ) ?: false
    }
}