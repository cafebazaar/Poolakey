package ir.cafebazaar.poolakey.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

internal class BillingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return

        // Append ".iab" to the action and copy extras
        val broadcastIntent = Intent().apply {
            action = intent.action?.let { "$it.iab" }
            intent.extras?.let { putExtras(it) }
        }

        notifyObserversSafely(broadcastIntent)
    }

    /**
     * Notify observers safely:
     * - Remove observers that throw exceptions
     * - Remove observers that have timed out
     * - Respect filterAction and priority
     */
    private fun notifyObserversSafely(intent: Intent) {
        val now = System.currentTimeMillis()

        synchronized(observerLock) {
            val iterator = observerEntries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()

                // Remove timed-out observers
                if (now - (observerTimestamps[entry.communicator] ?: 0) > OBSERVER_TIMEOUT_MS) {
                    Log.w(TAG, "Observer timed out and removed: ${entry.communicator}")
                    iterator.remove()
                    observerTimestamps.remove(entry.communicator)
                    continue
                }

                // Notify if action matches filter or no filter
                if (entry.filterAction == null || entry.filterAction == intent.action) {
                    try {
                        entry.communicator.onNewBroadcastReceived(intent)
                        observerTimestamps[entry.communicator] = now
                    } catch (e: Exception) {
                        Log.w(TAG, "Observer exception removed: ${entry.communicator} | ${e.message}")
                        iterator.remove()
                        observerTimestamps.remove(entry.communicator)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BillingReceiver"
        private val observerLock = Any()
        private val observerEntries = mutableListOf<ObserverEntry>()
        private val observerTimestamps = mutableMapOf<BillingReceiverCommunicator, Long>()
        private const val OBSERVER_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes

        // ===========================
        // Observer Management Methods
        // ===========================

        fun addObserver(
            communicator: BillingReceiverCommunicator,
            filterAction: String? = null,
            priority: Int = 0
        ) {
            synchronized(observerLock) {
                if (observerEntries.none { it.communicator == communicator }) {
                    observerEntries.add(ObserverEntry(communicator, filterAction, priority))
                    observerEntries.sortByDescending { it.priority } // Highest priority first
                    observerTimestamps[communicator] = System.currentTimeMillis()
                    Log.i(TAG, "Observer added: $communicator | Filter: ${filterAction ?: "ALL"} | Priority: $priority")
                }
            }
        }

        fun removeObserver(communicator: BillingReceiverCommunicator) {
            synchronized(observerLock) {
                observerEntries.removeAll { it.communicator == communicator }
                observerTimestamps.remove(communicator)
                Log.i(TAG, "Observer removed: $communicator")
            }
        }

        fun clearObservers() {
            synchronized(observerLock) {
                observerEntries.clear()
                observerTimestamps.clear()
                Log.i(TAG, "All observers cleared")
            }
        }

        fun listObservers(): List<BillingReceiverCommunicator> {
            synchronized(observerLock) {
                return observerEntries.map { it.communicator }
            }
        }

        fun isObserverRegistered(communicator: BillingReceiverCommunicator): Boolean {
            synchronized(observerLock) {
                return observerEntries.any { it.communicator == communicator }
            }
        }

        /**
         * Print a visually appealing observer table in Logcat
         */
        fun printRegisteredObservers() {
            synchronized(observerLock) {
                Log.i(TAG, "================ Registered Observers ================")
                if (observerEntries.isEmpty()) {
                    Log.i(TAG, "No observers registered")
                } else {
                    observerEntries.forEachIndexed { index, entry ->
                        val lastSeen = observerTimestamps[entry.communicator] ?: 0
                        val timeAgoSec = ((System.currentTimeMillis() - lastSeen) / 1000)
                        Log.i(
                            TAG, String.format(
                                "%2d | %-35s | Filter: %-10s | Priority: %2d | LastSeen: %4ds ago",
                                index + 1,
                                entry.communicator.toString(),
                                entry.filterAction ?: "ALL",
                                entry.priority,
                                timeAgoSec
                            )
                        )
                    }
                }
                Log.i(TAG, "=====================================================")
            }
        }
    }

    private data class ObserverEntry(
        val communicator: BillingReceiverCommunicator,
        val filterAction: String? = null,
        val priority: Int = 0
    )
}

// ========================================================
// BillingReceiverCommunicator Interface
// ========================================================

internal interface BillingReceiverCommunicator {
    fun onNewBroadcastReceived(intent: Intent?)
}
