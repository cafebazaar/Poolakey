package ir.cafebazaar.poolakey.billing.skudetail

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.BillingFunction
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.constant.Billing
import ir.cafebazaar.poolakey.entity.SkuDetails
import ir.cafebazaar.poolakey.exception.ResultNotOkayException
import ir.cafebazaar.poolakey.takeIf
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class GetSkuDetailFunction(
    private val context: Context,
    private val mainThread: PoolakeyThread<() -> Unit>
) : BillingFunction<SkuDetailFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: SkuDetailFunctionRequest
    ): Unit = with(request) {
        try {
            val skuBundle = Bundle().apply {
                putStringArrayList(
                    BazaarIntent.REQUEST_SKU_DETAILS_LIST,
                    ArrayList(request.skuIds)
                )
            }
            billingService.getSkuDetails(
                Billing.IN_APP_BILLING_VERSION,
                context.packageName,
                request.purchaseType.type,
                skuBundle
            )?.takeIfIsResponseOKOrThrowException(
                mainThread,
                callback
            )?.takeIfBundleContainsCorrectResponseKeyOrThrowException(
                mainThread,
                callback
            )?.let { bundle ->
                extractSkuDetailDataFromBundle(bundle, request.purchaseType)
            }?.also { items ->
                mainThread.execute {
                    GetSkuDetailsCallback().apply(callback).getSkuDetailsSucceed.invoke(items)
                }
            }
        } catch (e: RemoteException) {
            mainThread.execute {
                GetSkuDetailsCallback().apply(callback).getSkuDetailsFailed.invoke(e)
            }
        }
    }

    private fun extractSkuDetailDataFromBundle(
        bundle: Bundle,
        purchaseType: PurchaseType
    ): List<SkuDetails>? {
        return bundle.getStringArrayList(BazaarIntent.RESPONSE_GET_SKU_DETAILS_LIST)?.map {
            SkuDetails.fromJson(purchaseType, it)
        }
    }
}

private fun Bundle.takeIfBundleContainsCorrectResponseKeyOrThrowException(
    mainThread: PoolakeyThread<() -> Unit>,
    callback: GetSkuDetailsCallback.() -> Unit
): Bundle? {
    return takeIf(
        thisIsTrue = { bundle ->
            bundle.containsKey(BazaarIntent.RESPONSE_GET_SKU_DETAILS_LIST)
        },
        andIfNot = {
            mainThread.execute {
                GetSkuDetailsCallback().apply(callback)
                    .getSkuDetailsFailed
                    .invoke(IllegalStateException("Missing data from the received result"))
            }
        }
    )
}

private fun Bundle.takeIfIsResponseOKOrThrowException(
    mainThread: PoolakeyThread<() -> Unit>,
    callback: GetSkuDetailsCallback.() -> Unit
): Bundle? {
    return takeIf(
        thisIsTrue = { bundle ->
            bundle.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK
        },
        andIfNot = {
            mainThread.execute {
                GetSkuDetailsCallback().apply(callback)
                    .getSkuDetailsFailed
                    .invoke(ResultNotOkayException())
            }
        }
    )
}
