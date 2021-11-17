package ir.cafebazaar.poolakey.billing.trialsubscription

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import ir.cafebazaar.poolakey.billing.BillingFunction
import ir.cafebazaar.poolakey.billing.Feature
import ir.cafebazaar.poolakey.billing.FeatureConfig
import ir.cafebazaar.poolakey.callback.CheckTrialSubscriptionCallback
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.entity.TrialSubscriptionInfo
import ir.cafebazaar.poolakey.exception.BazaarNotSupportedException
import ir.cafebazaar.poolakey.exception.ResultNotOkayException
import ir.cafebazaar.poolakey.takeIf
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class CheckTrialSubscriptionFunction(
    private val context: Context,
    private val mainThread: PoolakeyThread<() -> Unit>
) : BillingFunction<CheckTrialSubscriptionFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: CheckTrialSubscriptionFunctionRequest
    ): Unit = with(request) {
        try {
            val isCheckTrialSubscriptionAvailable = FeatureConfig.isFeatureAvailable(
                featureConfigBundle = billingService.featureConfig,
                feature = Feature.CHECK_TRIAL_SUBSCRIPTION
            )
            if (isCheckTrialSubscriptionAvailable.not()) {
                CheckTrialSubscriptionCallback().apply(callback)
                    .checkTrialSubscriptionFailed
                    .invoke(BazaarNotSupportedException())
                return@with
            }

            billingService.checkTrialSubscription(
                context.packageName,
            )?.takeIfIsResponseOKOrThrowException(
                mainThread,
                callback
            )?.takeIfBundleContainsCorrectResponseKeyOrThrowException(
                mainThread,
                callback
            )?.let { bundle ->
                extractTrialSubscriptionDataFromBundle(bundle)
            }?.also { items ->
                mainThread.execute {
                    CheckTrialSubscriptionCallback().apply(callback)
                        .checkTrialSubscriptionSucceed
                        .invoke(items)
                }
            }
        } catch (e: RemoteException) {
            mainThread.execute {
                CheckTrialSubscriptionCallback().apply(callback)
                    .checkTrialSubscriptionFailed.invoke(e)
            }
        }
    }
}

internal fun extractTrialSubscriptionDataFromBundle(
    bundle: Bundle
): TrialSubscriptionInfo? {
    return bundle.getString(BazaarIntent.RESPONSE_CHECK_TRIAL_SUBSCRIPTION_DATA)?.let {
        TrialSubscriptionInfo.fromJson(it)
    }
}

private fun Bundle.takeIfBundleContainsCorrectResponseKeyOrThrowException(
    mainThread: PoolakeyThread<() -> Unit>,
    callback: CheckTrialSubscriptionCallback.() -> Unit
): Bundle? {
    return takeIf(
        thisIsTrue = { bundle ->
            bundle.containsKey(BazaarIntent.RESPONSE_CHECK_TRIAL_SUBSCRIPTION_DATA)
        },
        andIfNot = {
            mainThread.execute {
                CheckTrialSubscriptionCallback().apply(callback)
                    .checkTrialSubscriptionFailed
                    .invoke(IllegalStateException("Missing data from the received result"))
            }
        }
    )
}

private fun Bundle.takeIfIsResponseOKOrThrowException(
    mainThread: PoolakeyThread<() -> Unit>,
    callback: CheckTrialSubscriptionCallback.() -> Unit
): Bundle? {
    return takeIf(
        thisIsTrue = { bundle ->
            bundle.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK
        },
        andIfNot = {
            mainThread.execute {
                CheckTrialSubscriptionCallback().apply(callback)
                    .checkTrialSubscriptionFailed
                    .invoke(ResultNotOkayException())
            }
        }
    )
}
