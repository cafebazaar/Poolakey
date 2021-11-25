package ir.cafebazaar.poolakey.entity

import org.json.JSONObject

class TrialSubscriptionInfo private constructor(
    val isAvailable: Boolean,
    val trialPeriodDays: Int
) {

    override fun toString(): String {
        return """
            isAvailable = $isAvailable
            trialPeriodDays = $trialPeriodDays
        """.trimIndent()
    }

    companion object {

        internal fun fromJson(json: String): TrialSubscriptionInfo {
            val jsonObject = JSONObject(json)
            return with(jsonObject) {
                TrialSubscriptionInfo(
                    optBoolean("isAvailable"),
                    optInt("trialPeriodDays")
                )
            }
        }
    }
}