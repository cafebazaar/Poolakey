package com.phelat.poolakey.rx

import com.phelat.poolakey.Connection
import com.phelat.poolakey.Payment
import io.reactivex.Observable

/**
 * You have to use this function to connect to the In-App Billing service. Note that you have to
 * connect to Bazaar's Billing service before using any other available functions, So make sure
 * you call this function before doing anything else, also make sure that you are connected to
 * the billing service through Connection.
 * @see Connection
 * @return Observable that you can subscribe to it and get notified about service connection changes.
 */
fun Payment.connect(): Observable<Connection> {
    return Observable.create<Connection> { emitter ->
        connect {
            connectionSucceed { emitter.onNext(this) }
            disconnected { emitter.onNext(this) }
            connectionFailed { emitter.onError(it) }
        }
    }
}
