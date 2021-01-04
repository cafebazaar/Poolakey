package ir.cafebazaar.poolakey.billing.connection

import android.content.*
import android.os.Bundle
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.callback.ConsumeCallback
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.constant.Const
import ir.cafebazaar.poolakey.constant.Const.BAZAAR_PACKAGE_NAME
import ir.cafebazaar.poolakey.exception.BazaarNotSupportedException
import ir.cafebazaar.poolakey.exception.DisconnectException
import ir.cafebazaar.poolakey.exception.PurchaseHijackedException
import ir.cafebazaar.poolakey.getPackageInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.cafebazaar.poolakey.sdkAwareVersionCode
import ir.cafebazaar.poolakey.security.Security
import ir.cafebazaar.poolakey.takeIf
import java.lang.ref.WeakReference

internal class ReceiverBillingConnection(
    private val paymentConfiguration: PaymentConfiguration
) : BillingConnectionCommunicator {

    private var callbackReference: WeakReference<ConnectionCallback>? = null
    private var contextReference: WeakReference<Context>? = null

    private var receiverConnection: BroadcastReceiver? = null
    private var disconnected: Boolean = false

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
            trySendPingToBazaar()
            return true
        } else if (bazaarVersionCode > 0) {
            callback.connectionFailed.invoke(BazaarNotSupportedException())
        }

        return false
    }

    private fun canConnectWithReceiverComponent(bazaarVersionCode: Long): Boolean {
        return bazaarVersionCode > BAZAAR_WITH_RECEIVER_CONNECTION_VERSION
    }

    override fun isPurchaseTypeSupported(purchaseType: PurchaseType): Boolean {
        TODO("Not yet implemented")
    }

    override fun consume(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        TODO("Not yet implemented")
    }

    override fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun purchase(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit,
        fireIntentSender: (IntentSender) -> Unit,
        fireIntent: (Intent) -> Unit
    ) {
        TODO("Not yet implemented")
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
            ACTION_RECEIVE_PING -> {
                onPingActionReceived()
            }
        }
    }

    private fun onPingActionReceived() {
        callbackReference?.get()?.connectionSucceed?.invoke()
    }

    private fun isBundleSignatureValid(extras: Bundle?): Boolean {
        return getSecureSignature() == extras?.getString(KEY_SECURE)
    }

    private fun trySendPingToBazaar() {
        val intent: Intent = getNewIntentForBroadcast()
        intent.action = ACTION_PING
        contextReference?.get()?.sendBroadcast(intent)
    }

    private fun registerBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_PING)
        contextReference?.get()?.registerReceiver(receiverConnection, intentFilter)
    }

    private fun getNewIntentForBroadcast(): Intent {
        val bundle = Bundle().apply {
            putString(KEY_PACKAGE_NAME, contextReference?.get()?.packageName)
            putString(KEY_SECURE, getSecureSignature())
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

    companion object {
        private const val BAZAAR_WITH_RECEIVER_CONNECTION_VERSION = 801301

        private const val DEFAULT_SECURE_SIGNATURE = "secureBroadcastKey"
        private const val ACTION_BAZAAR_BASE = "com.farsitel.bazaar."
        private const val ACTION_BAZAAR_POST = ".iab"

        private const val ACTION_PING: String = ACTION_BAZAAR_BASE + "ping"
        private const val ACTION_RECEIVE_PING = ACTION_PING + ACTION_BAZAAR_POST

        private const val KEY_PACKAGE_NAME = "packageName"
        private const val KEY_SECURE = "secure"
    }
}