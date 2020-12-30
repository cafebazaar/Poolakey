package ir.cafebazaar.poolakey

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import ir.cafebazaar.poolakey.billing.BillingFunction
import ir.cafebazaar.poolakey.billing.PurchaseBillingConnection
import ir.cafebazaar.poolakey.billing.connection.BillingConnectionCommunicator
import ir.cafebazaar.poolakey.billing.connection.ServiceBillingConnection
import ir.cafebazaar.poolakey.billing.consume.ConsumeFunctionRequest
import ir.cafebazaar.poolakey.billing.purchase.PurchaseFunctionRequest
import ir.cafebazaar.poolakey.billing.query.QueryFunctionRequest
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.thread.PoolakeyThread

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration,
    private val backgroundThread: PoolakeyThread<Runnable>,
    private val purchaseFunction: BillingFunction<PurchaseFunctionRequest>,
    private val consumeFunction: BillingFunction<ConsumeFunctionRequest>,
    private val queryFunction: BillingFunction<QueryFunctionRequest>
) {

    private var callback: ConnectionCallback? = null

    private var billingCommunicator: BillingConnectionCommunicator? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)

        val serviceCommunicator = ServiceBillingConnection(
            backgroundThread,
            paymentConfiguration,
            purchaseFunction,
            consumeFunction,
            queryFunction
        )

        val canConnect = serviceCommunicator.startConnection(context, requireNotNull(callback))
        if (canConnect) {
            billingCommunicator = serviceCommunicator
        }
        return requireNotNull(callback)
    }

    fun purchase(
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        runOnCommunicator("purchase") {
            PurchaseBillingConnection.purchase(
                requireNotNull(billingCommunicator),
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
        runOnCommunicator("purchase") {
            PurchaseBillingConnection.purchase(
                requireNotNull(billingCommunicator),
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
        runOnCommunicator("consume") {
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
        runOnCommunicator("queryPurchasedProducts") {
            requireNotNull(billingCommunicator).queryPurchasedProducts(
                purchaseType,
                callback
            )
        }
    }

    private fun stopConnection() {
        runOnCommunicator("stopConnection") {
            requireNotNull(billingCommunicator).stopConnection()
            requireNotNull(billingCommunicator).disconnect()
            disconnect()
        }
    }

    private fun disconnect() {
        callback?.disconnected?.invoke()
        callback = null
        backgroundThread.dispose()
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
            IllegalStateException("You call $methodName but communicator is not initialized yet")
        )
    }
}