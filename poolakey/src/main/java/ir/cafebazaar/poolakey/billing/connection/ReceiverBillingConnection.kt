package ir.cafebazaar.poolakey.billing.connection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.purchase.PurchaseWeakHolder
import ir.cafebazaar.poolakey.billing.query.QueryFunction
import ir.cafebazaar.poolakey.billing.query.QueryFunctionRequest
import ir.cafebazaar.poolakey.billing.skudetail.SkuDetailFunctionRequest
import ir.cafebazaar.poolakey.billing.skudetail.extractSkuDetailDataFromBundle
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.GetSkuDetailsCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.constant.BazaarIntent.REQUEST_SKU_DETAILS_LIST
import ir.cafebazaar.poolakey.constant.Billing
import ir.cafebazaar.poolakey.constant.Const.BAZAAR_PACKAGE_NAME
import ir.cafebazaar.poolakey.exception.BazaarNotSupportedException
import ir.cafebazaar.poolakey.exception.ConsumeFailedException
import ir.cafebazaar.poolakey.exception.DisconnectException
import ir.cafebazaar.poolakey.exception.IAPNotSupportedException
import ir.cafebazaar.poolakey.exception.PurchaseHijackedException
import ir.cafebazaar.poolakey.exception.ResultNotOkayException
import ir.cafebazaar.poolakey.exception.SubsNotSupportedException
import ir.cafebazaar.poolakey.getPackageInfo
import ir.cafebazaar.poolakey.receiver.BillingReceiver
import ir.cafebazaar.poolakey.receiver.BillingReceiverCommunicator
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.request.purchaseExtraData
import ir.cafebazaar.poolakey.sdkAwareVersionCode
import ir.cafebazaar.poolakey.security.Security
import ir.cafebazaar.poolakey.takeIf
import java.lang.ref.WeakReference

internal class ReceiverBillingConnection(
    private val paymentConfiguration: PaymentConfiguration,
    private val queryFunction: QueryFunction
) : BillingConnectionCommunicator {

    private var consumeCallback: (ConsumeCallback.() -> Unit)? = null
    private var queryCallback: (PurchaseQueryCallback.() -> Unit)? = null
    private var skuDetailCallback: (GetSkuDetailsCallback.() -> Unit)? = null

    private var connectionCallbackReference: WeakReference<ConnectionCallback>? = null
    private var contextReference: WeakReference<Context>? = null

    private var receiverCommunicator: BillingReceiverCommunicator? = null
    private var disconnected: Boolean = false

    private var purchaseFragmentWeakReference: WeakReference<PurchaseWeakHolder<Fragment>>? = null
    private var purchaseActivityWeakReference: WeakReference<PurchaseWeakHolder<Activity>>? = null

    override fun startConnection(context: Context, callback: ConnectionCallback): Boolean {
        connectionCallbackReference = WeakReference(callback)
        contextReference = WeakReference(context)

        if (!Security.verifyBazaarIsInstalled(context)) {
            return false
        }

        val bazaarVersionCode = getPackageInfo(context, BAZAAR_PACKAGE_NAME)?.let {
            sdkAwareVersionCode(it)
        } ?: 0L

        return when {
            canConnectWithReceiverComponent(bazaarVersionCode) -> {
                createReceiverConnection()
                registerBroadcast()
                isPurchaseTypeSupported()
                true
            }
            bazaarVersionCode > 0 -> {
                callback.connectionFailed.invoke(BazaarNotSupportedException())
                false
            }
            else -> {
                false
            }
        }
    }

    private fun canConnectWithReceiverComponent(bazaarVersionCode: Long): Boolean {
        return bazaarVersionCode > BAZAAR_WITH_RECEIVER_CONNECTION_VERSION
    }

    private fun createReceiverConnection() {
        receiverCommunicator = object : BillingReceiverCommunicator {
            override fun onNewBroadcastReceived(intent: Intent?) {
                intent?.action?.takeIf(
                    thisIsTrue = {
                        isBundleSignatureValid(intent.extras)
                    },
                    andIfNot = {
                        connectionCallbackReference?.get()?.connectionFailed?.invoke(
                            PurchaseHijackedException()
                        )
                    }
                )?.takeIf(
                    thisIsTrue = {
                        !disconnected
                    },
                    andIfNot = {
                        connectionCallbackReference?.get()?.connectionFailed?.invoke(
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
            ACTION_RECEIVE_QUERY_PURCHASES -> {
                onQueryPurchaseReceived(extras)
            }
            ACTION_RECEIVE_SKU_DETAILS -> {
                onGetSkuDetailsReceived(extras)
            }
        }
    }

    private fun isPurchaseTypeSupported() {
        getNewIntentForBroadcast().apply {
            action = ACTION_BILLING_SUPPORT
        }.run(::sendBroadcast)
    }

    override fun consume(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        consumeCallback = callback

        getNewIntentForBroadcast().apply {
            action = ACTION_CONSUME
            putExtra(KEY_TOKEN, purchaseToken)
        }.run(::sendBroadcast)
    }

    override fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) {
        queryCallback = callback
        getNewIntentForBroadcast().apply {
            action = ACTION_QUERY_PURCHASES
            putExtra(KEY_ITEM_TYPE, purchaseType.type)
        }.run(::sendBroadcast)
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

    override fun getSkuDetails(
        request: SkuDetailFunctionRequest,
        callback: GetSkuDetailsCallback.() -> Unit
    ) {
        skuDetailCallback = callback
        getNewIntentForBroadcast().apply {
            action = ACTION_GET_SKU_DETAIL
            putExtra(KEY_ITEM_TYPE, request.purchaseType.type)
            putStringArrayListExtra(REQUEST_SKU_DETAILS_LIST, ArrayList(request.skuIds))
        }.run(::sendBroadcast)
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
            putExtra(KEY_EXTRA_INFO, purchaseRequest.purchaseExtraData())
        }.run(::sendBroadcast)
    }

    override fun stopConnection() {
        disconnected = true

        clearReferences()

        receiverCommunicator?.let(BillingReceiver::removeObserver)
        receiverCommunicator = null
    }

    private fun clearReferences() {
        consumeCallback = null
        queryCallback = null
        skuDetailCallback = null
        connectionCallbackReference = null
        contextReference = null

        purchaseFragmentWeakReference?.clear()
        purchaseFragmentWeakReference = null

        purchaseActivityWeakReference?.clear()
        purchaseActivityWeakReference = null
    }

    private fun onQueryPurchaseReceived(extras: Bundle?) {
        queryCallback?.let {
            queryFunction.function(
                QueryFunctionRequest(
                    purchaseType = "",
                    queryBundle = { _, _ -> extras },
                    callback = it
                )
            )
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
                    // invalid state, we receive purchase but all reference is null, might be connection disconnected
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

    private fun onGetSkuDetailsReceived(extras: Bundle?) {
        if (skuDetailCallback == null) {
            return
        }

        if (isResponseSucceed(extras)) {
            val response = extractSkuDetailDataFromBundle(requireNotNull(extras))
            GetSkuDetailsCallback()
                .apply(requireNotNull(skuDetailCallback))
                .getSkuDetailsSucceed.invoke(requireNotNull(response))
        } else {
            GetSkuDetailsCallback().apply(requireNotNull(skuDetailCallback)).run {
                getSkuDetailsFailed.invoke(ResultNotOkayException())
            }
        }
    }

    private fun onBillingSupportActionReceived(extras: Bundle?) {
        val isResponseSucceed = isResponseSucceed(extras)
        val isSubscriptionSupport = isSubscriptionSupport(extras)

        when {
            isResponseSucceed && isSubscriptionSupport -> {
                connectionCallbackReference?.get()?.connectionSucceed?.invoke()
            }
            !isResponseSucceed -> {
                connectionCallbackReference?.get()?.connectionFailed?.invoke(
                    IAPNotSupportedException()
                )
            }
            else -> {
                connectionCallbackReference?.get()?.connectionFailed?.invoke(
                    SubsNotSupportedException()
                )
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
        BillingReceiver.addObserver(requireNotNull(receiverCommunicator))
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
        private const val ACTION_CONSUME = ACTION_BAZAAR_BASE + "consume"
        private const val ACTION_PURCHASE = ACTION_BAZAAR_BASE + "purchase"
        private const val ACTION_QUERY_PURCHASES = ACTION_BAZAAR_BASE + "getPurchase"
        private const val ACTION_GET_SKU_DETAIL = ACTION_BAZAAR_BASE + "skuDetail"

        private const val ACTION_RECEIVE_CONSUME = ACTION_CONSUME + ACTION_BAZAAR_POST
        private const val ACTION_RECEIVE_PURCHASE = ACTION_PURCHASE + ACTION_BAZAAR_POST
        private const val ACTION_RECEIVE_BILLING_SUPPORT =
            ACTION_BILLING_SUPPORT + ACTION_BAZAAR_POST
        private const val ACTION_RECEIVE_QUERY_PURCHASES =
            ACTION_QUERY_PURCHASES + ACTION_BAZAAR_POST
        private const val ACTION_RECEIVE_SKU_DETAILS =
            ACTION_GET_SKU_DETAIL + ACTION_BAZAAR_POST

        private const val KEY_SUBSCRIPTION_SUPPORT = "subscriptionSupport"
        private const val KEY_PACKAGE_NAME = "packageName"
        private const val KEY_API_VERSION = "apiVersion"
        private const val KEY_SKU = "sku"
        private const val KEY_ITEM_TYPE = "itemType"
        private const val KEY_DEVELOPER_PAYLOAD = "developerPayload"
        private const val KEY_EXTRA_INFO = "extraInfo"
        private const val KEY_SECURE = "secure"
        private const val KEY_RESPONSE_BUY_INTENT = "BUY_INTENT"
        private const val RESPONSE_CODE = "RESPONSE_CODE"
        private const val KEY_TOKEN = "token"
    }
}