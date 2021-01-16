package ir.cafebazaar.poolakey.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal class BillingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Intent().apply {
            action = intent!!.action + ".iab"
            intent.extras?.let { bundle ->
                putExtras(bundle)
            }
        }.run {
            notifyObservers(this)
        }
    }

    private fun notifyObservers(intent: Intent) {
        synchronized(observerLock) {
            for (observer in observers) {
                observer.onNewBroadcastReceived(intent)
            }
        }
    }

    companion object {

        private val observerLock = Object()
        private val observers = mutableListOf<BillingReceiverCommunicator>()

        fun addObserver(communicator: BillingReceiverCommunicator) {
            synchronized(observerLock) {
                observers.add(communicator)
            }
        }

        fun removeObserver(communicator: BillingReceiverCommunicator) {
            synchronized(observerLock) {
                observers.remove(communicator)
            }
        }
    }
}