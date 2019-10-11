package com.phelat.poolakeysample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.phelat.poolakey.Connection
import com.phelat.poolakey.ConnectionState
import com.phelat.poolakey.Payment
import com.phelat.poolakey.request.PurchaseRequest
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val payment by lazy(LazyThreadSafetyMode.NONE) {
        Payment(context = this)
    }

    private lateinit var paymentConnection: Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        purchaseButton.setOnClickListener {
            if (paymentConnection.getState() == ConnectionState.Connected) {
                payment.purchaseItem(
                    activity = this,
                    request = PurchaseRequest(
                        productId = skuValueInput.text.toString(),
                        requestCode = PURCHASE_REQUEST_CODE,
                        payload = ""
                    )
                )
            }
        }
        subscribeButton.setOnClickListener {
            if (paymentConnection.getState() == ConnectionState.Connected) {
                payment.subscribeItem(
                    activity = this@MainActivity,
                    request = PurchaseRequest(
                        productId = skuValueInput.text.toString(),
                        requestCode = SUBSCRIBE_REQUEST_CODE,
                        payload = ""
                    )
                )
            }
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
        payment.consumeItem(purchaseToken) {
            consumeSucceed {
                toast(R.string.general_consume_succeed_message)
            }
            consumeFailed {
                toast(R.string.general_consume_failed_message)
            }
        }
    }

    private fun toast(@StringRes message: Int) {
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
