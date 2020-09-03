package ir.cafebazaar.poolakey.exception

class DisconnectException : IllegalStateException() {

    override val message: String?
        get() = "We can't communicate with Bazaar: Service is disconnected"

}
