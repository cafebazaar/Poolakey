package com.phelat.poolakeysample

import android.os.Bundle
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

    override fun onDestroy() {
        paymentConnection.disconnect()
        super.onDestroy()
    }

    companion object {
        private const val PURCHASE_REQUEST_CODE = 1000
    }

}
