package ir.cafebazaar.poolakey.billing.query

import android.os.Bundle
import android.os.RemoteException
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.exception.ResultNotOkayException
import ir.cafebazaar.poolakey.mapper.RawDataToPurchaseInfo
import ir.cafebazaar.poolakey.security.PurchaseVerifier
import ir.cafebazaar.poolakey.takeIf
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class QueryFunction(
    private val rawDataToPurchaseInfo: RawDataToPurchaseInfo,
    private val purchaseVerifier: PurchaseVerifier,
    private val paymentConfiguration: PaymentConfiguration,
    private val mainThread: PoolakeyThread<() -> Unit>,
) {

    fun function(request: QueryFunctionRequest): Unit = with(request) {
        try {
            var continuationToken: String? = null
            do {
                queryBundle(purchaseType, continuationToken)?.takeIf(
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