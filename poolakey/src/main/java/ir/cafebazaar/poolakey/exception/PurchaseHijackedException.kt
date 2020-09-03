package ir.cafebazaar.poolakey.exception

import java.lang.Exception

class PurchaseHijackedException : Exception() {

    override val message: String?
        get() = "The purchase was hijacked and it's not a valid purchase"

}
