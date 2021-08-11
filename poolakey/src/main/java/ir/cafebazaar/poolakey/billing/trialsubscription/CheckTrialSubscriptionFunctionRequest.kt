package ir.cafebazaar.poolakey.billing.trialsubscription

import ir.cafebazaar.poolakey.billing.FunctionRequest
import ir.cafebazaar.poolakey.callback.CheckTrialSubscriptionCallback

internal class CheckTrialSubscriptionFunctionRequest(
    val callback: CheckTrialSubscriptionCallback.() -> Unit
) : FunctionRequest