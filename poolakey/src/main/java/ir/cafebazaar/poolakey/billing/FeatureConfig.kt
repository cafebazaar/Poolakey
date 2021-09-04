package ir.cafebazaar.poolakey.billing

import android.os.Bundle

internal object FeatureConfig {

    internal fun isFeatureAvailable(
        featureConfigBundle: Bundle?,
        feature: Feature
    ): Boolean {
        return featureConfigBundle?.getBoolean(
            feature.key,
            false
        ) ?: false
    }
}

internal enum class Feature(val key: String) {
    CHECK_TRIAL_SUBSCRIPTION("INTENT_TRIAL_SUBSCRIPTION_SUPPORT")
}