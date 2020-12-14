package ir.cafebazaar.poolakey.callback

class ConsumeCallback {

    internal var consumeSucceed: () -> Unit = {}

    internal var consumeFailed: (throwable: Throwable) -> Unit = {}

    fun consumeSucceed(block: () -> Unit) {
        consumeSucceed = block
    }

    fun consumeFailed(block: (throwable: Throwable) -> Unit) {
        consumeFailed = block
    }

}
