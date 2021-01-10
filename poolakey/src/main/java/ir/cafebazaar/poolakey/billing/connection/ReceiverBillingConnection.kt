package ir.cafebazaar.poolakey.billing.connection

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.purchase.PurchaseWeakHolder
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.constant.Billing
import ir.cafebazaar.poolakey.constant.Const
import ir.cafebazaar.poolakey.constant.Const.BAZAAR_PACKAGE_NAME
import ir.cafebazaar.poolakey.exception.*
import ir.cafebazaar.poolakey.getPackageInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.sdkAwareVersionCode
import ir.cafebazaar.poolakey.security.Security
import ir.cafebazaar.poolakey.takeIf
import ir.cafebazaar.poolakey.thread.PoolakeyThread
import java.lang.ref.WeakReference

internal class ReceiverBillingConnection(
    private val paymentConfiguration: PaymentConfiguration,
    private val backgroundThread: PoolakeyThread<Runnable>,
) : BillingConnectionCommunicator {

    private var consumeCallback: (ConsumeCallback.() -> Unit)? = null
    private var callbackReference: WeakReference<ConnectionCallback>? = null
    private var contextReference: WeakReference<Context>? = null

    private var receiverConnection: BroadcastReceiver? = null
    private var disconnected: Boolean = false

    private var purchaseFragmentWeakReference: WeakReference<PurchaseWeakHolder<Fragment>>? = null
    private var purchaseActivityWeakReference: WeakReference<PurchaseWeakHolder<Activity>>? = null

    override fun startConnection(context: Context, callback: ConnectionCallback): Boolean {
        callbackReference = WeakReference(callback)
        contextReference = WeakReference(context)

        if (!Security.verifyBazaarIsInstalled(context)) {
            return false
        }

        val bazaarVersionCode = getPackageInfo(context, Const.BAZAAR_PACKAGE_NAME)?.let {
            sdkAwareVersionCode(it)
        } ?: 0L

        if (canConnectWithReceiverComponent(bazaarVersionCode)) {
            createReceiverConnection()
            registerBroadcast()
            backgroundThread.execute(Runnable { isPurchaseTypeSupported() })
            return true
        } else if (bazaarVersionCode > 0) {
            callback.connectionFailed.invoke(BazaarNotSupportedException())
        }

        return false
    }

    private fun canConnectWithReceiverComponent(bazaarVersionCode: Long): Boolean {
        return bazaarVersionCode > BAZAAR_WITH_RECEIVER_CONNECTION_VERSION
    }

    private fun isPurchaseTypeSupported() {
        getNewIntentForBroadcast().apply {
            action = ACTION_BILLING_SUPPORT
        }.run {
            sendBroadcast(this)
        }
    }

    override fun consume(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        consumeCallback = callback

        getNewIntentForBroadcast().apply {
            action = ACTION_CONSUME
            putExtra(KEY_TOKEN, purchaseToken)
        }.run {
            sendBroadcast(this)
        }
    }

    override fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun purchase(
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        purchaseActivityWeakReference = WeakReference(
            PurchaseWeakHolder(activity, purchaseRequest.requestCode, callback)
        )

        sendPurchaseBroadcast(purchaseRequest, purchaseType, callback)
    }

    override fun purchase(
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        purchaseFragmentWeakReference = WeakReference(
            PurchaseWeakHolder(fragment, purchaseRequest.requestCode, callback)
        )

        sendPurchaseBroadcast(purchaseRequest, purchaseType, callback)
    }

    private fun sendPurchaseBroadcast(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        getNewIntentForBroadcast().apply {
            action = ACTION_PURCHASE
            putExtra(KEY_SKU, purchaseRequest.productId)
            putExtra(KEY_DEVELOPER_PAYLOAD, purchaseRequest.payload)
            putExtra(KEY_ITEM_TYPE, purchaseType.type)

        }.run {
            sendBroadcast(this)
        }
    }


    override fun stopConnection() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        disconnected = true
    }

    private fun createReceiverConnection() {
        receiverConnection = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.action?.takeIf(
                    thisIsTrue = {
                        isBundleSignatureValid(intent.extras)
                    },
                    andIfNot = {
                        callbackReference?.get()?.connectionFailed?.invoke(
                            PurchaseHijackedException()
                        )
                    }
                )?.takeIf(
                    thisIsTrue = {
                        disconnected
                    },
                    andIfNot = {
                        callbackReference?.get()?.connectionFailed?.invoke(
                            DisconnectException()
                        )
                    }
                )?.let { action ->
                    onActionReceived(action, intent.extras)
                }
            }
        }
    }

    private fun onActionReceived(action: String, extras: Bundle?) {
        when (action) {
            ACTION_RECEIVE_BILLING_SUPPORT -> {
                onBillingSupportActionReceived(extras)
            }
            ACTION_RECEIVE_CONSUME -> {
                onConsumeActionReceived(extras)
            }
            ACTION_RECEIVE_PURCHASE -> {
                onPurchaseReceived(extras)
            }
        }
    }

    private fun onPurchaseReceived(extras: Bundle?) {
        if (isResponseSucceed(extras)) {
            when {
                purchaseActivityWeakReference?.get() != null -> {
                    startPurchaseActivityWithActivity(
                        requireNotNull(purchaseActivityWeakReference?.get()),
                        getPurchaseIntent(extras)
                    )
                }
                purchaseFragmentWeakReference?.get() != null -> {
                    startPurchaseActivityWithFragment(
                        requireNotNull(purchaseFragmentWeakReference?.get()),
                        getPurchaseIntent(extras)
                    )
                }
                else -> {
                    // TODO handle this
                }
            }
        } else {
            getPurchaseCallback()?.let { purchaseCallback ->
                PurchaseIntentCallback()
                    .apply(purchaseCallback)
                    .failedToBeginFlow.invoke(DisconnectException())
            }
        }
    }

    private fun getPurchaseCallback(): (PurchaseIntentCallback.() -> Unit)? {
        return when {
            purchaseActivityWeakReference?.get() != null -> {
                purchaseActivityWeakReference?.get()?.callback
            }
            purchaseFragmentWeakReference?.get() != null -> {
                purchaseActivityWeakReference?.get()?.callback
            }
            else -> {
                null
            }
        }
    }

    private fun startPurchaseActivityWithActivity(
        purchaseWeakHolder: PurchaseWeakHolder<Activity>,
        purchaseIntent: Intent?
    ) {
        purchaseWeakHolder.component.startActivityForResult(
            purchaseIntent,
            purchaseWeakHolder.requestCode
        )
    }

    private fun startPurchaseActivityWithFragment(
        purchaseWeakHolder: PurchaseWeakHolder<Fragment>,
        purchaseIntent: Intent?
    ) {
        purchaseWeakHolder.component.startActivityForResult(
            purchaseIntent,
            purchaseWeakHolder.requestCode
        )
    }

    private fun getPurchaseIntent(extras: Bundle?): Intent? {
        return extras?.getParcelable(KEY_RESPONSE_BUY_INTENT)
    }

    private fun onConsumeActionReceived(extras: Bundle?) {
        if (consumeCallback == null) {
            return
        }

        ConsumeCallback().apply(requireNotNull(consumeCallback)).run {
            if (isResponseSucceed(extras)) {
                consumeSucceed.invoke()
            } else {
                consumeFailed.invoke(ConsumeFailedException())
            }
        }
    }

    private fun onBillingSupportActionReceived(extras: Bundle?) {
        val isResponseSucceed = isResponseSucceed(extras)
        val isSubscriptionSupport = isSubscriptionSupport(extras)

        when {
            isResponseSucceed && isSubscriptionSupport -> {
                callbackReference?.get()?.connectionSucceed?.invoke()
            }
            !isResponseSucceed -> {
                callbackReference?.get()?.connectionFailed?.invoke(IAPNotSupportedException())
            }
            else -> {
                callbackReference?.get()?.connectionFailed?.invoke(SubsNotSupportedException())
            }
        }
    }

    private fun isSubscriptionSupport(extras: Bundle?): Boolean {
        val isSubscriptionSupport = extras?.getBoolean(KEY_SUBSCRIPTION_SUPPORT) ?: false
        return !paymentConfiguration.shouldSupportSubscription || isSubscriptionSupport
    }

    private fun isResponseSucceed(extras: Bundle?): Boolean {
        return extras?.getInt(RESPONSE_CODE) == BazaarIntent.RESPONSE_RESULT_OK
    }

    private fun isBundleSignatureValid(extras: Bundle?): Boolean {
        return getSecureSignature() == extras?.getString(KEY_SECURE)
    }

    private fun registerBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_BILLING_SUPPORT)
        intentFilter.addAction(ACTION_RECEIVE_CONSUME)
        intentFilter.addAction(ACTION_RECEIVE_PURCHASE)
        contextReference?.get()?.registerReceiver(receiverConnection, intentFilter)
    }

    private fun getNewIntentForBroadcast(): Intent {
        val bundle = Bundle().apply {
            putString(KEY_PACKAGE_NAME, contextReference?.get()?.packageName)
            putString(KEY_SECURE, getSecureSignature())
            putInt(KEY_API_VERSION, Billing.IN_APP_BILLING_VERSION)
        }
        return Intent().apply {
            `package` = BAZAAR_PACKAGE_NAME
            putExtras(bundle)
        }
    }

    private fun getSecureSignature(): String {
        val rsaKey = (paymentConfiguration.localSecurityCheck as? SecurityCheck.Enable)
            ?.rsaPublicKey

        return rsaKey ?: DEFAULT_SECURE_SIGNATURE
    }

    private fun sendBroadcast(intent: Intent) {
        contextReference?.get()?.sendBroadcast(intent)
    }

    companion object {
        private const val BAZAAR_WITH_RECEIVER_CONNECTION_VERSION = 801301

        private const val DEFAULT_SECURE_SIGNATURE = "secureBroadcastKey"
        private const val ACTION_BAZAAR_BASE = "com.farsitel.bazaar."
        private const val ACTION_BAZAAR_POST = ".iab"

        private const val ACTION_BILLING_SUPPORT = ACTION_BAZAAR_BASE + "billingSupport"
        private const val ACTION_CONSUME: String = ACTION_BAZAAR_BASE + "consume"
        private const val ACTION_PURCHASE: String = ACTION_BAZAAR_BASE + "purchase"

        private const val ACTION_RECEIVE_BILLING_SUPPORT =
            ACTION_BILLING_SUPPORT + ACTION_BAZAAR_POST

        private const val ACTION_RECEIVE_CONSUME = ACTION_CONSUME + ACTION_BAZAAR_POST
        private const val ACTION_RECEIVE_PURCHASE = ACTION_PURCHASE + ACTION_BAZAAR_POST

        private const val KEY_SUBSCRIPTION_SUPPORT = "subscriptionSupport"
        private const val KEY_PACKAGE_NAME = "packageName"
        private const val KEY_API_VERSION = "apiVersion"
        private const val KEY_SKU = "sku"
        private const val KEY_ITEM_TYPE = "itemType"
        private const val KEY_DEVELOPER_PAYLOAD = "developerPayload"
        private const val KEY_SECURE = "secure"
        private const val KEY_RESPONSE_BUY_INTENT = "BUY_INTENT"
        private const val RESPONSE_CODE = "RESPONSE_CODE"
        private const val KEY_TOKEN = "token"
    }
}