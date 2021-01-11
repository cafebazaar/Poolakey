package ir.cafebazaar.poolakey.receiver

import android.content.Intent

interface BillingReceiverCommunicator {
    fun onNewBroadcastReceived(intent: Intent?)
}
