package ir.cafebazaar.poolakey.exception

class ResultNotOkayException : IllegalStateException() {

    override val message: String?
        get() = "Failed to receive response from Bazaar"

}
