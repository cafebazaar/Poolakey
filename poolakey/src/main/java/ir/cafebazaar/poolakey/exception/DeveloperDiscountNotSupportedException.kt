package ir.cafebazaar.poolakey.exception

class DeveloperDiscountNotSupportedException : IllegalStateException() {

    override val message: String
        get() = "Developer discount not supported"

}
