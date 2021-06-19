package ir.cafebazaar.poolakeysample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.ConnectionState
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.callback.PurchaseQueryCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.exception.DynamicPriceNotSupportedException
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.android.synthetic.main.activity_main.consumeSwitch
import kotlinx.android.synthetic.main.activity_main.dynamicPriceToken
import kotlinx.android.synthetic.main.activity_main.getSkuDetailInAppButton
import kotlinx.android.synthetic.main.activity_main.getSkuDetailSubscriptionButton
import kotlinx.android.synthetic.main.activity_main.purchaseButton
import kotlinx.android.synthetic.main.activity_main.queryPurchasedItemsButton
import kotlinx.android.synthetic.main.activity_main.querySubscribedItemsButton
import kotlinx.android.synthetic.main.activity_main.serviceConnectionStatus
import kotlinx.android.synthetic.main.activity_main.skuValueInput
import kotlinx.android.synthetic.main.activity_main.subscribeButton

class MainActivity : AppCompatActivity() {

    private val paymentConfiguration = PaymentConfiguration(
        localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = BuildConfig.IN_APP_BILLING_KEY)
    )

    private val payment by lazy(LazyThreadSafetyMode.NONE) {
        Payment(context = this, config = paymentConfiguration)
    }

    private lateinit var paymentConnection: Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startPaymentConnection()
        setViewClickListener()
    }

    private fun setViewClickListener() {
        purchaseButton.setOnClickListener {
            if (paymentConnection.getState() == ConnectionState.Connected) {
                purchaseProduct(
                    productId = skuValueInput.text.toString(),
                    requestCode = PURCHASE_REQUEST_CODE,
                    payload = "payload",
                    dynamicPriceToken = dynamicPriceToken.text.toString()
                )
            }
        }
        subscribeButton.setOnClickListener {
            if (paymentConnection.getState() == ConnectionState.Connected) {
                subscribeProduct(
                    productId = skuValueInput.text.toString(),
                    requestCode = SUBSCRIBE_REQUEST_CODE,
                    payload = "",
                    dynamicPriceToken = dynamicPriceToken.text.toString()
                )
            }
        }
        queryPurchasedItemsButton.setOnClickListener {
            if (paymentConnection.getState() == ConnectionState.Connected) {
                payment.getPurchasedProducts(handlePurchaseQueryCallback())
            }
        }
        querySubscribedItemsButton.setOnClickListener {
            if (paymentConnection.getState() == ConnectionState.Connected) {
                payment.getSubscribedProducts(handlePurchaseQueryCallback())
            }
        }
        setGetSkuDetailClickListener()
    }

    private fun subscribeProduct(
        productId: String,
        requestCode: Int,
        payload: String,
        dynamicPriceToken: String?
    ) {
        payment.subscribeProduct(
            activity = this@MainActivity,
            request = PurchaseRequest(
                productId = skuValueInput.text.toString(),
                requestCode = SUBSCRIBE_REQUEST_CODE,
                payload = "",
                dynamicPriceToken = dynamicPriceToken
            )
        ) {
            purchaseFlowBegan {
                toast(R.string.general_purchase_flow_began_message)
            }
            failedToBeginFlow {
                // bazaar need to update, in this case we only launch purchase without discount
                if (it is DynamicPriceNotSupportedException) {
                    toast(R.string.general_purchase_failed_dynamic_price_token_message)
                    subscribeProduct(productId, requestCode, payload, null)
                } else {
                    toast(R.string.general_purchase_failed_message)
                }
            }
        }
    }

    private fun purchaseProduct(
        productId: String,
        requestCode: Int,
        payload: String,
        dynamicPriceToken: String?
    ) {
        payment.purchaseProduct(
            activity = this,
            request = PurchaseRequest(
                productId = productId,
                requestCode = requestCode,
                payload = payload,
                dynamicPriceToken = dynamicPriceToken
            )
        ) {
            purchaseFlowBegan {
                toast(R.string.general_purchase_flow_began_message)
            }
            failedToBeginFlow {
                // bazaar need to update, in this case we only launch purchase without discount
                if (it is DynamicPriceNotSupportedException) {
                    toast(R.string.general_purchase_failed_dynamic_price_token_message)
                    purchaseProduct(productId, requestCode, payload, null)
                } else {
                    toast(R.string.general_purchase_failed_message)
                }
            }
        }
    }

    private fun setGetSkuDetailClickListener() {

        getSkuDetailInAppButton.setOnClickListener {
            onGetSkuDetailInAppClicked()
        }

        getSkuDetailSubscriptionButton.setOnClickListener {
            onGetSkuDetailSubscriptionClicked()
        }
    }

    private fun onGetSkuDetailSubscriptionClicked() {
        if (paymentConnection.getState() == ConnectionState.Connected) {
            payment.getSubscriptionSkuDetails(
                skuIds = listOf(skuValueInput.text.toString())
            ) {
                getSkuDetailsSucceed {
                    toast(it.toString())
                }
                getSkuDetailsFailed {
                    toast(R.string.general_query_get_sku_detail_failed_message)
                }
            }
        }
    }

    private fun onGetSkuDetailInAppClicked() {
        if (paymentConnection.getState() == ConnectionState.Connected) {
            payment.getInAppSkuDetails(
                skuIds = listOf(skuValueInput.text.toString())
            ) {
                getSkuDetailsSucceed {
                    toast(it.toString())
                }
                getSkuDetailsFailed {
                    toast(R.string.general_query_get_sku_detail_failed_message)
                }
            }
        }
    }

    private fun startPaymentConnection() {
        paymentConnection = payment.connect {
            connectionSucceed {
                serviceConnectionStatus.setText(R.string.general_service_connection_connected_text)
            }
            connectionFailed {
                serviceConnectionStatus.setText(R.string.general_service_connection_failed_text)
            }
            disconnected {
                serviceConnectionStatus.setText(R.string.general_service_connection_not_connected_text)
            }
        }
    }

    private fun handlePurchaseQueryCallback(): PurchaseQueryCallback.() -> Unit = {
        querySucceed { purchasedItems ->
            val productId = skuValueInput.text.toString()
            purchasedItems.find { it.productId == productId }
                ?.also { toast(R.string.general_user_purchased_item_message) }
                ?: run { toast(R.string.general_user_did_not_purchased_item_message) }
        }
        queryFailed {
            toast(R.string.general_query_purchased_items_failed_message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        payment.onActivityResult(requestCode, resultCode, data) {
            purchaseSucceed {
                toast(R.string.general_purchase_succeed_message)
                if (consumeSwitch.isChecked) {
                    consumePurchasedItem(it.purchaseToken)
                }
            }
            purchaseCanceled {
                toast(R.string.general_purchase_cancelled_message)
            }
            purchaseFailed {
                toast(R.string.general_purchase_failed_message)
            }
        }
    }

    private fun consumePurchasedItem(purchaseToken: String) {
        payment.consumeProduct(purchaseToken) {
            consumeSucceed {
                toast(R.string.general_consume_succeed_message)
            }
            consumeFailed {
                toast(R.string.general_consume_failed_message)
            }
        }
    }

    private fun toast(@StringRes message: Int) {
        toast(getString(message))
    }

    private fun toast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        paymentConnection.disconnect()
        super.onDestroy()
    }

    companion object {
        private const val PURCHASE_REQUEST_CODE = 1000
        private const val SUBSCRIBE_REQUEST_CODE = 1001
    }

}
