package com.phelat.poolakey

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.fragment.app.Fragment
import com.android.vending.billing.IInAppBillingService
import com.phelat.poolakey.callback.ConnectionCallback
import com.phelat.poolakey.callback.ConsumeCallback
import com.phelat.poolakey.callback.PurchaseIntentCallback
import com.phelat.poolakey.callback.PurchaseQueryCallback
import com.phelat.poolakey.config.PaymentConfiguration
import com.phelat.poolakey.config.SecurityCheck
import com.phelat.poolakey.constant.BazaarIntent
import com.phelat.poolakey.entity.PurchaseInfo
import com.phelat.poolakey.exception.BazaarNotFoundException
import com.phelat.poolakey.exception.ConsumeFailedException
import com.phelat.poolakey.exception.DisconnectException
import com.phelat.poolakey.exception.IAPNotSupportedException
import com.phelat.poolakey.exception.ResultNotOkayException
import com.phelat.poolakey.exception.SubsNotSupportedException
import com.phelat.poolakey.mapper.RawDataToPurchaseInfo
import com.phelat.poolakey.request.PurchaseRequest
import com.phelat.poolakey.security.PurchaseVerifier
import com.phelat.poolakey.thread.PoolakeyThread

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration,
    private val rawDataToPurchaseInfo: RawDataToPurchaseInfo,
    private val purchaseVerifier: PurchaseVerifier,
    private val backgroundThread: PoolakeyThread<Runnable>,
    private val mainThread: PoolakeyThread<() -> Unit>
) : ServiceConnection {

    private var callback: ConnectionCallback? = null

    private var billingService: IInAppBillingService? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)
        Intent(BILLING_SERVICE_ACTION).apply { `package` = BAZAAR_PACKAGE_NAME }
            .takeIf(
                thisIsTrue = {
                    context.packageManager.queryIntentServices(it, 0).isNotEmpty()
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(BazaarNotFoundException())
                }
            )
            ?.also {
                try {
                    context.bindService(it, this, Context.BIND_AUTO_CREATE)
                } catch (e: SecurityException) {
                    callback?.connectionFailed?.invoke(e)
                }
            }
        return requireNotNull(callback)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        IInAppBillingService.Stub.asInterface(service)
            ?.takeIf(
                thisIsTrue = {
                    isPurchaseTypeSupported(
                        purchaseType = PurchaseType.IN_APP,
                        inAppBillingService = it
                    )
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(IAPNotSupportedException())
                }
            )
            ?.takeIf(
                thisIsTrue = {
                    !paymentConfiguration.shouldSupportSubscription || isPurchaseTypeSupported(
                        purchaseType = PurchaseType.SUBSCRIPTION,
                        inAppBillingService = it
                    )
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(SubsNotSupportedException())
                }
            )
            ?.also { billingService = it }
            ?.also { callback?.connectionSucceed?.invoke() }
    }

    private fun isPurchaseTypeSupported(
        purchaseType: PurchaseType,
        inAppBillingService: IInAppBillingService
    ): Boolean {
        val supportState = inAppBillingService.isBillingSupported(
            IN_APP_BILLING_VERSION,
            context.packageName,
            purchaseType.type
        )
        return supportState == BazaarIntent.RESPONSE_RESULT_OK
    }

    fun purchase(
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        purchase(purchaseRequest, purchaseType, callback) { intentSender ->
            activity.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }
    }

    fun purchase(
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        purchase(purchaseRequest, purchaseType, callback) { intentSender ->
            fragment.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0,
                null
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }
    }

    private fun purchase(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit,
        fireIntent: (IntentSender) -> Unit
    ) = withService {
        try {
            getBuyIntent(
                IN_APP_BILLING_VERSION,
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
    } ifServiceIsDisconnected {
        PurchaseIntentCallback().apply(callback).failedToBeginFlow.invoke(DisconnectException())
    }

    fun consume(
        purchaseToken: String,
        callback: ConsumeCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        try {
            consumePurchase(IN_APP_BILLING_VERSION, context.packageName, purchaseToken)
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
    } ifServiceIsDisconnected {
        ConsumeCallback().apply(callback).consumeFailed.invoke(DisconnectException())
    }

    fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        try {
            var continuationToken: String? = null
            do {
                getPurchases(
                    IN_APP_BILLING_VERSION,
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
    } ifServiceIsDisconnected {
        PurchaseQueryCallback().apply(callback).queryFailed.invoke(DisconnectException())
    }

    private fun extractPurchasedDataFromBundle(bundle: Bundle): List<PurchaseInfo> {
        val purchaseDataList: List<String> = bundle.getStringArrayList(
            BazaarIntent.RESPONSE_PURCHASE_DATA_LIST
        ) ?: emptyList()
        val signatureDataList: List<String> = bundle.getStringArrayList(
            BazaarIntent.RESPONSE_DATA_SIGNATURE_LIST
        ) ?: emptyList()
        val validPurchases = mutableListOf<PurchaseInfo>()
        for (i in purchaseDataList.indices) {
            if (paymentConfiguration.localSecurityCheck is SecurityCheck.Enable) {
                val isPurchaseValid = purchaseVerifier.verifyPurchase(
                    paymentConfiguration.localSecurityCheck.rsaPublicKey,
                    purchaseDataList[i],
                    signatureDataList[i]
                )
                if (!isPurchaseValid) continue
            }
            validPurchases += rawDataToPurchaseInfo.mapToPurchaseInfo(purchaseDataList[i])
        }
        return validPurchases
    }

    private fun stopConnection() {
        if (billingService != null) {
            context.unbindService(this)
            disconnect()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        disconnect()
    }

    private fun disconnect() {
        billingService = null
        callback?.disconnected?.invoke()
        callback = null
        backgroundThread.dispose()
    }

    private inline fun withService(
        runOnBackground: Boolean = false,
        crossinline service: IInAppBillingService.() -> Unit
    ): ConnectionState {
        return billingService?.also {
            if (runOnBackground) {
                backgroundThread.execute(Runnable { service.invoke(it) })
            } else {
                service.invoke(it)
            }
        }?.let { ConnectionState.Connected }
            ?: run { ConnectionState.Disconnected }
    }

    private inline infix fun ConnectionState.ifServiceIsDisconnected(block: () -> Unit) {
        if (this is ConnectionState.Disconnected) {
            block.invoke()
        }
    }

    companion object {
        private const val IN_APP_BILLING_VERSION = 3
        private const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
        private const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
        private const val INTENT_RESPONSE_BUY = "BUY_INTENT"
    }

}
