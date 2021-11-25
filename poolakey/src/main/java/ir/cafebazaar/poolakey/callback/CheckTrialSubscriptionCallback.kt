package ir.cafebazaar.poolakey.callback

import ir.cafebazaar.poolakey.entity.TrialSubscriptionInfo

class CheckTrialSubscriptionCallback {

    internal var checkTrialSubscriptionSucceed: (TrialSubscriptionInfo) -> Unit = {}

    internal var checkTrialSubscriptionFailed: (throwable: Throwable) -> Unit = {}

    fun checkTrialSubscriptionSucceed(block: (TrialSubscriptionInfo) -> Unit) {
        checkTrialSubscriptionSucceed = block
    }

    fun checkTrialSubscriptionFailed(block: (throwable: Throwable) -> Unit) {
        checkTrialSubscriptionFailed = block
    }
}