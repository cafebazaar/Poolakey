package com.phelat.poolakey.exception

class SubsNotSupportedException : IllegalAccessException() {

    override val message: String?
        get() = "Subscription is not supported in this version of installed Bazaar"

}
