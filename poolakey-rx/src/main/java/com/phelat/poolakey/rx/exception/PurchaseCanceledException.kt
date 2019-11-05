package com.phelat.poolakey.rx.exception

import java.lang.Exception

class PurchaseCanceledException : Exception() {

    override val message: String?
        get() = "Purchase canceled by user"

}
