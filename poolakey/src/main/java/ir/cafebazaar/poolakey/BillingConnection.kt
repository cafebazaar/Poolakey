package ir.cafebazaar.poolakey

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import ir.cafebazaar.poolakey.billing.BillingFunction
import ir.cafebazaar.poolakey.billing.connection.BillingConnectionCommunicator
import ir.cafebazaar.poolakey.billing.connection.ReceiverBillingConnection
import ir.cafebazaar.poolakey.billing.connection.ServiceBillingConnection
import ir.cafebazaar.poolakey.billing.skudetail.SkuDetailFunctionRequest
import ir.cafebazaar.poolakey.billing.query.QueryFunction
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration,
    private val backgroundThread: PoolakeyThread<Runnable>,
    private val queryFunction: QueryFunction,
    private val skuDetailFunction: BillingFunction<SkuDetailFunctionRequest>
    private val mainThread: PoolakeyThread<() -> Unit>
) {

    private var callback: ConnectionCallback? = null

    private var billingCommunicator: BillingConnectionCommunicator? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)

        val serviceCommunicator = ServiceBillingConnection(
            context,
            mainThread,
            backgroundThread,
            paymentConfiguration,
            queryFunction,
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
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_PURCHASE) {
            requireNotNull(billingCommunicator).purchase(
                activity,
                purchaseRequest,
                purchaseType,
                callback
            )
        }
    }

    fun purchase(
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_PURCHASE) {
            requireNotNull(billingCommunicator).purchase(
                fragment,
                purchaseRequest,
                purchaseType,
                callback
            )
        }
    }

    fun consume(
        purchaseToken: String,
        callback: ConsumeCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_CONSUME) {
            requireNotNull(billingCommunicator).consume(
                purchaseToken,
                callback
            )
        }
    }

    fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) {
        runOnCommunicator(TAG_QUERY_PURCHASE_PRODUCT) {
            requireNotNull(billingCommunicator).queryPurchasedProducts(
                purchaseType,
                callback
            )
        }
    }

    fun getSkuDetail(
        purchaseType: PurchaseType,
        skuIds: List<String>,
        callback: GetSkuDetailsCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        skuDetailFunction.function(
            billingService = this,
            request = SkuDetailFunctionRequest(purchaseType, skuIds, callback)
        )
    }

    private fun stopConnection() {
        runOnCommunicator(TAG_STOP_CONNECTION) {
            requireNotNull(billingCommunicator).stopConnection()
            disconnect()
        }
    }

    private fun disconnect() {
        callback?.disconnected?.invoke()
        callback = null
        backgroundThread.dispose()
        billingCommunicator = null
    }

    private fun runOnCommunicator(
        methodName: String,
        ifConnected: () -> Unit
    ) {
        if (billingCommunicator == null) {
            raiseErrorForCommunicatorNotInitialized(methodName)
        } else {
            ifConnected.invoke()
        }
    }

    private fun raiseErrorForCommunicatorNotInitialized(methodName: String) {
        callback?.connectionFailed?.invoke(
            IllegalStateException("You called $methodName but communicator is not initialized yet")
        )
    }

    companion object {
        private const val TAG_STOP_CONNECTION = "stopConnection"
        private const val TAG_QUERY_PURCHASE_PRODUCT = "queryPurchasedProducts"
        private const val TAG_CONSUME = "consume"
        private const val TAG_PURCHASE = "purchase"
    }
}