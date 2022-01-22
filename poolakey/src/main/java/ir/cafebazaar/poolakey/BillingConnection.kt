package ir.cafebazaar.poolakey

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.billing.connection.BillingConnectionCommunicator
import ir.cafebazaar.poolakey.billing.connection.ReceiverBillingConnection
import ir.cafebazaar.poolakey.billing.connection.ServiceBillingConnection
import ir.cafebazaar.poolakey.billing.query.QueryFunction
import ir.cafebazaar.poolakey.billing.skudetail.GetSkuDetailFunction
import ir.cafebazaar.poolakey.billing.skudetail.SkuDetailFunctionRequest
import ir.cafebazaar.poolakey.billing.trialsubscription.CheckTrialSubscriptionFunction
import ir.cafebazaar.poolakey.billing.trialsubscription.CheckTrialSubscriptionFunctionRequest
import ir.cafebazaar.poolakey.callback.CheckTrialSubscriptionCallback
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.callback.PurchaseCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration,
    private val backgroundThread: PoolakeyThread<Runnable>,
    private val queryFunction: QueryFunction,
    private val skuDetailFunction: GetSkuDetailFunction,
    private val purchaseResultParser: PurchaseResultParser,
    private val checkTrialSubscriptionFunction: CheckTrialSubscriptionFunction,
    private val mainThread: PoolakeyThread<() -> Unit>
) {

    private var callback: ConnectionCallback? = null
    private var paymentLauncher: PaymentLauncher? = null

    private var billingCommunicator: BillingConnectionCommunicator? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)

        val serviceCommunicator = ServiceBillingConnection(
            context,
            mainThread,
            backgroundThread,
            paymentConfiguration,
            queryFunction,
            skuDetailFunction,
            checkTrialSubscriptionFunction,
            ::disconnect
        )

        val receiverConnection = ReceiverBillingConnection(
            paymentConfiguration,
            queryFunction
        )

        val canConnect = serviceCommunicator.startConnection(context, requireNotNull(callback))

        billingCommunicator = if (canConnect) {
            serviceCommunicator
        } else {
            receiverConnection.startConnection(
                context,
                requireNotNull(callback)
            )

            receiverConnection
        }
        return requireNotNull(callback)
    }

    fun purchase(
        registry: ActivityResultRegistry,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        paymentLauncher = PaymentLauncher.Builder(registry) {
            onActivityResult(it, purchaseCallback)
        }.build()

        runOnCommunicator(TAG_PURCHASE) { billingCommunicator ->
            billingCommunicator.purchase(
                requireNotNull(paymentLauncher),
                purchaseRequest,
                purchaseType,
                purchaseCallback
            )
        }
    }

    fun consume(
        purchaseToken: String,
        callback: ConsumeCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_CONSUME) { billingCommunicator ->
            billingCommunicator.consume(
                purchaseToken,
                callback
            )
        }
    }

    fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_QUERY_PURCHASE_PRODUCT) { billingCommunicator ->
            billingCommunicator.queryPurchasedProducts(
                purchaseType,
                callback
            )
        }
    }

    fun getSkuDetail(
        purchaseType: PurchaseType,
        skuIds: List<String>,
        callback: GetSkuDetailsCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_GET_SKU_DETAIL) { billingCommunicator ->
            billingCommunicator.getSkuDetails(
                SkuDetailFunctionRequest(purchaseType, skuIds, callback),
                callback
            )
        }
    }

    fun checkTrialSubscription(
        callback: CheckTrialSubscriptionCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_CHECK_TRIAL_SUBSCRIPTION) { billingCommunicator ->
            billingCommunicator.checkTrialSubscription(
                CheckTrialSubscriptionFunctionRequest(callback),
                callback
            )
        }
    }

    private fun stopConnection() {
        runOnCommunicator(TAG_STOP_CONNECTION) { billingCommunicator ->
            billingCommunicator.stopConnection()
            disconnect()
        }
    }

    private fun disconnect() {
        callback?.disconnected?.invoke()
        callback = null
        paymentLauncher?.unregister()
        paymentLauncher = null
        backgroundThread.dispose()
        billingCommunicator = null
    }

    private fun runOnCommunicator(
        methodName: String,
        ifConnected: (BillingConnectionCommunicator) -> Unit
    ) {
        billingCommunicator?.let(ifConnected)
            ?: raiseErrorForCommunicatorNotInitialized(methodName)
    }

    private fun raiseErrorForCommunicatorNotInitialized(methodName: String) {
        callback?.connectionFailed?.invoke(
            IllegalStateException("You called $methodName but communicator is not initialized yet")
        )
    }

    private fun onActivityResult(
        activityResult: ActivityResult,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        when (activityResult.resultCode) {
            Activity.RESULT_OK -> {
                purchaseResultParser.handleReceivedResult(
                    paymentConfiguration.localSecurityCheck,
                    activityResult.data,
                    purchaseCallback
                )
            }
            Activity.RESULT_CANCELED -> {
                PurchaseCallback().apply(purchaseCallback)
                    .purchaseCanceled
                    .invoke()
            }
            else -> {
                PurchaseCallback().apply(purchaseCallback)
                    .purchaseFailed
                    .invoke(IllegalStateException("Result code is not valid"))
            }
        }
    }

    companion object {

        const val PAYMENT_SERVICE_KEY = "payment_service_key"

        private const val TAG_STOP_CONNECTION = "stopConnection"
        private const val TAG_QUERY_PURCHASE_PRODUCT = "queryPurchasedProducts"
        private const val TAG_CONSUME = "consume"
        private const val TAG_PURCHASE = "purchase"
        private const val TAG_GET_SKU_DETAIL = "skuDetial"
        private const val TAG_CHECK_TRIAL_SUBSCRIPTION = "checkTrialSubscription"
    }
}