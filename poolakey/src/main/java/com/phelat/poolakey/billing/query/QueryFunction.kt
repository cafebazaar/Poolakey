package com.phelat.poolakey.billing.query

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import com.phelat.poolakey.billing.BillingFunction
import com.phelat.poolakey.callback.PurchaseQueryCallback
import com.phelat.poolakey.config.PaymentConfiguration
import com.phelat.poolakey.config.SecurityCheck
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.constant.Billing
import com.phelat.poolakey.entity.PurchaseInfo
import com.phelat.poolakey.exception.ResultNotOkayException
import com.phelat.poolakey.mapper.RawDataToPurchaseInfo
import com.phelat.poolakey.security.PurchaseVerifier
import com.phelat.poolakey.takeIf
import com.phelat.poolakey.thread.PoolakeyThread

internal class QueryFunction(
    private val rawDataToPurchaseInfo: RawDataToPurchaseInfo,
    private val purchaseVerifier: PurchaseVerifier,
    private val paymentConfiguration: PaymentConfiguration,
    private val mainThread: PoolakeyThread<() -> Unit>,
    private val context: Context
) : BillingFunction<QueryFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: QueryFunctionRequest
    ): Unit = with(request) {
        try {
            var continuationToken: String? = null
            do {
                billingService.getPurchases(
                    Billing.IN_APP_BILLING_VERSION,
                    context.packageName,
                    purchaseType.type,
                    continuationToken
                )?.takeIf(
                    thisIsTrue = { bundle ->
                        bundle.get(BazaarIntent.RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK
                    },
                    andIfNot = {
                        mainThread.execute {
                            PurchaseQueryCallback().apply(callback)
                                .queryFailed
                                .invoke(ResultNotOkayException())
                        }
                    }
                )?.takeIf(
                    thisIsTrue = { bundle ->
                        bundle.containsKey(BazaarIntent.RESPONSE_PURCHASE_ITEM_LIST)
                            .and(bundle.containsKey(BazaarIntent.RESPONSE_PURCHASE_DATA_LIST))
                            .and(bundle.containsKey(BazaarIntent.RESPONSE_DATA_SIGNATURE_LIST))
                            .and(bundle.getStringArrayList(BazaarIntent.RESPONSE_PURCHASE_DATA_LIST) != null)
                    },
                    andIfNot = {
                        mainThread.execute {
                            PurchaseQueryCallback().apply(callback)
                                .queryFailed
                                .invoke(IllegalStateException("Missing data from the received result"))
                        }
                    }
                )?.also { bundle ->
                    continuationToken = bundle.getString(BazaarIntent.RESPONSE_CONTINUATION_TOKEN)
                }?.let(::extractPurchasedDataFromBundle)?.also { purchasedItems ->
                    mainThread.execute {
                        PurchaseQueryCallback().apply(callback).querySucceed.invoke(purchasedItems)
                    }
                }
            } while (!continuationToken.isNullOrBlank())
        } catch (e: RemoteException) {
            mainThread.execute {
                PurchaseQueryCallback().apply(callback).queryFailed.invoke(e)
            }
        }
    }

    private fun extractPurchasedDataFromBundle(bundle: Bundle): List<PurchaseInfo> {
        val purchaseDataList: List<String> = bundle.getStringArrayList(
            BazaarIntent.RESPONSE_PURCHASE_DATA_LIST
        ) ?: emptyList()
        val signatureDataList: List<String> = bundle.getStringArrayList(
            BazaarIntent.RESPONSE_DATA_SIGNATURE_LIST
        ) ?: emptyList()
        val validPurchases = ArrayList<PurchaseInfo>(purchaseDataList.size)
        for (i in purchaseDataList.indices) {
            if (paymentConfiguration.localSecurityCheck is SecurityCheck.Enable) {
                val isPurchaseValid = purchaseVerifier.verifyPurchase(
                    paymentConfiguration.localSecurityCheck.rsaPublicKey,
                    purchaseDataList[i],
                    signatureDataList[i]
                )
                if (!isPurchaseValid) continue
            }
            validPurchases += rawDataToPurchaseInfo.mapToPurchaseInfo(
                purchaseDataList[i],
                signatureDataList[i]
            )
        }
        return validPurchases
    }

}