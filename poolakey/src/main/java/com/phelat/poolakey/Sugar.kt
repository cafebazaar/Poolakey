package com.phelat.poolakey

internal inline fun <T> T?.takeIf(thisIsTrue: (T?) -> Boolean, andIfNot: () -> Unit): T? {
    if (!thisIsTrue.invoke(this)) {
        andIfNot.invoke()
    }
    return this
}
