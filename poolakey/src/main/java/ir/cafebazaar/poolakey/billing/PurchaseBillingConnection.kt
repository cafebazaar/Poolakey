package ir.cafebazaar.poolakey.billing

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.fragment.app.Fragment
import ir.cafebazaar.poolakey.PurchaseType
import ir.cafebazaar.poolakey.billing.connection.BillingConnectionCommunicator
import ir.cafebazaar.poolakey.callback.PurchaseIntentCallback
import ir.cafebazaar.poolakey.request.PurchaseRequest


internal object PurchaseBillingConnection {

    fun purchase(
        billingConnectionCommunicator: BillingConnectionCommunicator,
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {

        val intentSenderFire: (IntentSender) -> Unit = { intentSender ->
            activity.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        val intentFire: (Intent) -> Unit = { intent ->
            activity.startActivityForResult(
                intent,
                purchaseRequest.requestCode
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        billingConnectionCommunicator.purchase(
            purchaseRequest,
            purchaseType,
            callback,
            intentSenderFire,
            intentFire
        )
    }

    fun purchase(
        billingConnectionCommunicator: BillingConnectionCommunicator,
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        val intentSenderFire: (IntentSender) -> Unit = { intentSender ->
            fragment.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0,
                null
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        val intentFire: (Intent) -> Unit = { intent ->
            fragment.startActivityForResult(
                intent,
                purchaseRequest.requestCode
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        billingConnectionCommunicator.purchase(
            purchaseRequest,
            purchaseType,
            callback,
            intentSenderFire,
            intentFire
        )
    }
}