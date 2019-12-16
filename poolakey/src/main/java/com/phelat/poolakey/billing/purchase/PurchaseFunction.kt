package com.phelat.poolakey.billing.purchase

import android.app.PendingIntent
import android.content.Context
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import com.phelat.poolakey.billing.BillingFunction
import com.phelat.poolakey.callback.PurchaseIntentCallback
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.constant.Billing
import com.phelat.poolakey.exception.ResultNotOkayException
import com.phelat.poolakey.takeIf

internal class PurchaseFunction(
    private val context: Context
) : BillingFunction<PurchaseFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: PurchaseFunctionRequest
    ): Unit = with (request) {
        try {
            billingService.getBuyIntent(
                Billing.IN_APP_BILLING_VERSION,
                context.packageName,
                purchaseRequest.productId,
                purchaseType.type,
                purchaseRequest.payload
            )?.takeIf(
                thisIsTrue = { bundle ->
                    bundle.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK
                }, andIfNot = {
                    PurchaseIntentCallback().apply(callback)
                        .failedToBeginFlow
                        .invoke(ResultNotOkayException())
                }
            )?.takeIf(
                thisIsTrue = { bundle ->
                    bundle.getParcelable<PendingIntent>(INTENT_RESPONSE_BUY) != null
                }, andIfNot = {
                    PurchaseIntentCallback().apply(callback)
                        .failedToBeginFlow
                        .invoke(IllegalStateException("Couldn't receive buy intent from Bazaar"))
                }
            )?.getParcelable<PendingIntent>(INTENT_RESPONSE_BUY)?.also { purchaseIntent ->
                fireIntent.invoke(purchaseIntent.intentSender)
            }
        } catch (e: RemoteException) {
            PurchaseIntentCallback().apply(callback).failedToBeginFlow.invoke(e)
        }
    }

    companion object {
        private const val INTENT_RESPONSE_BUY = "BUY_INTENT"
    }

}