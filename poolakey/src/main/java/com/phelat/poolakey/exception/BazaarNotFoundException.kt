package com.phelat.poolakey.exception

class BazaarNotFoundException : IllegalStateException() {

    override val message: String?
        get() = "Bazaar is not installed"

}
