package com.phelat.poolakeysample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.phelat.poolakey.Connection
import com.phelat.poolakey.ConnectionState
import com.phelat.poolakey.Payment
import com.phelat.poolakey.PurchaseRequest
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
                        sku = skuValueInput.text.toString(),
                        requestCode = PURCHASE_REQUEST_CODE
                    )
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        payment.onActivityResult(requestCode, resultCode, data) {
            purchaseSucceed {
                Toast.makeText(
                    this@MainActivity,
                    R.string.general_purchase_succeed_message,
                    Toast.LENGTH_LONG
                ).show()
            }
            purchaseCanceled {
                Toast.makeText(
                    this@MainActivity,
                    R.string.general_purchase_cancelled_message,
                    Toast.LENGTH_LONG
                ).show()
            }
            purchaseFailed {
                Toast.makeText(
                    this@MainActivity,
                    R.string.general_purchase_failed_message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        paymentConnection.disconnect()
        super.onDestroy()
    }

    companion object {
        private const val PURCHASE_REQUEST_CODE = 1000
    }

}
