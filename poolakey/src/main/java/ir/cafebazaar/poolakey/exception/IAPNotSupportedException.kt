package ir.cafebazaar.poolakey.exception

class IAPNotSupportedException : IllegalAccessException() {

    override val message: String?
        get() = "In app billing is not supported in this version of installed Bazaar"

}
