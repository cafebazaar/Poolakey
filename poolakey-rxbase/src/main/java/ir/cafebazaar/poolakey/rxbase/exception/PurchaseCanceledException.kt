package ir.cafebazaar.poolakey.rxbase.exception

import java.lang.Exception

class PurchaseCanceledException : Exception() {

    override val message: String
        get() = "Purchase canceled by user"

}
