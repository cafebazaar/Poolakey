package ir.cafebazaar.poolakey

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.billing.query.QueryFunction
import ir.cafebazaar.poolakey.billing.skudetail.GetSkuDetailFunction
import ir.cafebazaar.poolakey.billing.trialsubscription.CheckTrialSubscriptionFunction
import ir.cafebazaar.poolakey.callback.*
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.mapper.RawDataToPurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.security.PurchaseVerifier
import ir.cafebazaar.poolakey.thread.BackgroundThread
import ir.cafebazaar.poolakey.thread.MainThread
import ir.cafebazaar.poolakey.thread.PoolakeyThread

class Payment(
    context: Context,
    config: PaymentConfiguration
) {

    private val backgroundThread: PoolakeyThread<Runnable> = BackgroundThread()
    private val mainThread: PoolakeyThread<() -> Unit> = MainThread()

    private val purchaseVerifier = PurchaseVerifier()
    private val rawDataToPurchaseInfo = RawDataToPurchaseInfo()

    private val queryFunction = QueryFunction(
        rawDataToPurchaseInfo,
        purchaseVerifier,
        config,
        mainThread,
    )

    private val getSkuFunction = GetSkuDetailFunction(
        context,
        mainThread
    )

    private val checkTrialSubscriptionFunction = CheckTrialSubscriptionFunction(
        context,
        mainThread
    )

    private val purchaseResultParser = PurchaseResultParser(rawDataToPurchaseInfo, purchaseVerifier)

    private val connection = BillingConnection(
        context = context,
        paymentConfiguration = config,
        queryFunction = queryFunction,
        backgroundThread = backgroundThread,
        skuDetailFunction = getSkuFunction,
        purchaseResultParser = purchaseResultParser,
        checkTrialSubscriptionFunction = checkTrialSubscriptionFunction,
        mainThread = mainThread
    )

    fun connect(callback: ConnectionCallback.() -> Unit): Connection {
        return connection.startConnection(callback)
    }

    fun purchaseProduct(
        registry: ActivityResultRegistry,
        request: PurchaseRequest,
        callback: PurchaseCallback.() -> Unit
    ) {
        connection.purchase(registry, request, PurchaseType.IN_APP, callback)
    }

    fun consumeProduct(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        connection.consume(purchaseToken, callback)
    }

    fun subscribeProduct(
        registry: ActivityResultRegistry,
        request: PurchaseRequest,
        callback: PurchaseCallback.() -> Unit
    ) {
        connection.purchase(registry, request, PurchaseType.SUBSCRIPTION, callback)
    }

    fun getPurchasedProducts(callback: PurchaseQueryCallback.() -> Unit) {
        connection.queryPurchasedProducts(PurchaseType.IN_APP, callback)
    }

    fun getSubscribedProducts(callback: PurchaseQueryCallback.() -> Unit) {
        connection.queryPurchasedProducts(PurchaseType.SUBSCRIPTION, callback)
    }

    fun getInAppSkuDetails(
        skuIds: List<String>,
        callback: GetSkuDetailsCallback.() -> Unit
    ) {
        connection.getSkuDetail(PurchaseType.IN_APP, skuIds, callback)
    }

    fun getSubscriptionSkuDetails(
        skuIds: List<String>,
        callback: GetSkuDetailsCallback.() -> Unit
    ) {
        connection.getSkuDetail(PurchaseType.SUBSCRIPTION, skuIds, callback)
    }

    fun checkTrialSubscription(
        callback: CheckTrialSubscriptionCallback.() -> Unit
    ) {
        connection.checkTrialSubscription(callback)
    }


    // -------------------- Additional Helper Functions --------------------

    fun refreshConnection(callback: ConnectionCallback.() -> Unit) {
        println("🔄 Refreshing connection to billing service...")
        connection.disconnect()
        connection.startConnection(callback)
    }

    fun isProductPurchased(sku: String, callback: (Boolean) -> Unit) {
        getPurchasedProducts {
            onQueryCompleted = { purchasedList ->
                val purchased = purchasedList.any { it.sku == sku }
                println("🔍 SKU '$sku' purchased? $purchased")
                callback(purchased)
            }
        }
    }

    fun isSubscriptionActive(sku: String, callback: (Boolean) -> Unit) {
        getSubscribedProducts {
            onQueryCompleted = { subscribedList ->
                val active = subscribedList.any { it.sku == sku && it.isAutoRenewing }
                println("🔍 Subscription '$sku' active? $active")
                callback(active)
            }
        }
    }

    fun restorePurchases(
        onRestoreCompleted: (purchasedProducts: List<String>, subscriptions: List<String>) -> Unit
    ) {
        val restoredProducts = mutableListOf<String>()
        val restoredSubscriptions = mutableListOf<String>()

        getPurchasedProducts {
            onQueryCompleted = { purchasedList ->
                restoredProducts.addAll(purchasedList.map { it.sku })

                getSubscribedProducts {
                    onQueryCompleted = { subscribedList ->
                        restoredSubscriptions.addAll(subscribedList.map { it.sku })
                        println("✅ Restored purchases: $restoredProducts")
                        println("🎉 Restored subscriptions: $restoredSubscriptions")
                        onRestoreCompleted(restoredProducts, restoredSubscriptions)
                    }
                }
            }
        }
    }

    fun logAllPurchasesAndSubscriptions() {
        getPurchasedProducts {
            onQueryCompleted = { purchasedList ->
                println("📦 Purchased products:")
                purchasedList.forEach { println(" - SKU: ${it.sku}, Token: ${it.purchaseToken}") }

                getSubscribedProducts {
                    onQueryCompleted = { subscribedList ->
                        println("✨ Active subscriptions:")
                        subscribedList.forEach { println(" - SKU: ${it.sku}, AutoRenew: ${it.isAutoRenewing}") }
                    }
                }
            }
        }
    }

    fun prefetchSkuDetails(
        inAppSkuIds: List<String> = emptyList(),
        subscriptionSkuIds: List<String> = emptyList(),
        callback: () -> Unit = {}
    ) {
        var pendingCalls = 0

        fun checkDone() {
            pendingCalls--
            if (pendingCalls <= 0) callback()
        }

        if (inAppSkuIds.isNotEmpty()) {
            pendingCalls++
            getInAppSkuDetails(inAppSkuIds) { onSkuDetailsReceived = { checkDone() } }
        }

        if (subscriptionSkuIds.isNotEmpty()) {
            pendingCalls++
            getSubscriptionSkuDetails(subscriptionSkuIds) { onSkuDetailsReceived = { checkDone() } }
        }

        if (pendingCalls == 0) callback()
    }
}
 
