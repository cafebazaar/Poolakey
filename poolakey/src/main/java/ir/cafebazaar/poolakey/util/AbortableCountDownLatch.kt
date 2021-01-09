package ir.cafebazaar.poolakey.util

import ir.cafebazaar.poolakey.exception.AbortedException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AbortableCountDownLatch(count: Int) : CountDownLatch(count) {
    private var aborted = false

    /**
     * Unblocks all threads waiting on this latch and cause them to receive an
     * AbortedException.  If the latch has already counted all the way down,
     * this method does nothing.
     */
    fun abort() {
        if (count == 0L) {
            return
        }
        aborted = true
        while (count > 0) {
            countDown()
        }
    }

    @Throws(InterruptedException::class)
    override fun await(timeout: Long, unit: TimeUnit): Boolean {
        val awaitForSuper = super.await(timeout, unit)
        if (aborted) {
            throw AbortedException()
        }
        return awaitForSuper
    }

    @Throws(InterruptedException::class)
    override fun await() {
        super.await()
        if (aborted) {
            throw AbortedException()
        }
    }
}