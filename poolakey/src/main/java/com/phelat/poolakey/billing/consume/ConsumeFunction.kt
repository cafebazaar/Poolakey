package com.phelat.poolakey.billing.consume

import android.content.Context
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import com.phelat.poolakey.billing.BillingFunction
import com.phelat.poolakey.callback.ConsumeCallback
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.constant.Billing
import com.phelat.poolakey.exception.ConsumeFailedException
import com.phelat.poolakey.takeIf
import com.phelat.poolakey.thread.PoolakeyThread

internal class ConsumeFunction(
    private val mainThread: PoolakeyThread<() -> Unit>,
    private val context: Context
) : BillingFunction<ConsumeFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: ConsumeFunctionRequest
    ): Unit = with(request) {
        try {
            billingService.consumePurchase(Billing.IN_APP_BILLING_VERSION, context.packageName, purchaseToken)
                .takeIf(
                    thisIsTrue = { it == BazaarIntent.RESPONSE_RESULT_OK },
                    andIfNot = {
                        mainThread.execute {
                            ConsumeCallback().apply(callback)
                                .consumeFailed
                                .invoke(ConsumeFailedException())
                        }
                    }
                )
                ?.also {
                    mainThread.execute {
                        ConsumeCallback().apply(callback).consumeSucceed.invoke()
                    }
                }
        } catch (e: RemoteException) {
            mainThread.execute {
                ConsumeCallback().apply(callback).consumeFailed.invoke(e)
            }
        }
    }

}