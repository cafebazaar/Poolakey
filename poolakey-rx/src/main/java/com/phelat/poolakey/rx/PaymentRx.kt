package com.phelat.poolakey.rx

import android.app.Activity
import androidx.fragment.app.Fragment
import com.phelat.poolakey.Connection
import com.phelat.poolakey.Payment
import com.phelat.poolakey.request.PurchaseRequest
import io.reactivex.Completable
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

/**
 * You can use this function to navigate user to Bazaar's payment activity to purchase a product.
 * Note that for subscribing a product you have to use the 'subscribeProduct' function.
 * @see subscribeProduct
 * @param activity We use this activity instance to actually start Bazaar's payment activity.
 * @param request This contains some information about the product that we are going to purchase.
 * @return Completable that you can subscribe to it and it gets completed when purchase flow begins.
 */
fun Payment.purchaseProduct(activity: Activity, request: PurchaseRequest): Completable {
    return Completable.create { emitter ->
        purchaseProduct(activity, request) {
            purchaseFlowBegan { emitter.onComplete() }
            failedToBeginFlow { emitter.onError(it) }
        }
    }
}

/**
 * You can use this function to navigate user to Bazaar's payment activity to purchase a product.
 * Note that for subscribing a product you have to use the 'subscribeProduct' function.
 * @see subscribeProduct
 * @param fragment We use this fragment instance to actually start Bazaar's payment activity.
 * @param request This contains some information about the product that we are going to purchase.
 * @return Completable that you can subscribe to it and it gets completed when purchase flow begins.
 */
fun Payment.purchaseProduct(fragment: Fragment, request: PurchaseRequest): Completable {
    return Completable.create { emitter ->
        purchaseProduct(fragment, request) {
            purchaseFlowBegan { emitter.onComplete() }
            failedToBeginFlow { emitter.onError(it) }
        }
    }
}

/**
 * You can use this function to consume an already purchased product. Note that you can't use
 * this function to consume subscribed products. This function runs off the main thread, so you
 * don't have to handle the threading by your self.
 * @param purchaseToken You have received this token when user purchased that particular product.
 * You can also use 'getPurchasedProducts' function to get all the purchased products by this
 * particular user.
 * @see getPurchasedProducts
 * @return Completable that you can subscribe to it and it gets completed when consumption succeeds.
 */
fun Payment.consumeProduct(purchaseToken: String): Completable {
    return Completable.create { emitter ->
        consumeProduct(purchaseToken) {
            consumeSucceed { emitter.onComplete() }
            consumeFailed { emitter.onError(it) }
        }
    }
}

/**
 * You can use this function to navigate user to Bazaar's payment activity to subscribe a product.
 * Note that for purchasing a product you have to use the 'purchaseProduct' function.
 * @see purchaseProduct
 * @param activity We use this activity instance to actually start Bazaar's payment activity.
 * @param request This contains some information about the product that we are going to subscribe.
 * @return Completable that you can subscribe to it and it gets completed when purchase flow begins.
 */
fun Payment.subscribeProduct(activity: Activity, request: PurchaseRequest): Completable {
    return Completable.create { emitter ->
        subscribeProduct(activity, request) {
            purchaseFlowBegan { emitter.onComplete() }
            failedToBeginFlow { emitter.onError(it) }
        }
    }
}

/**
 * You can use this function to navigate user to Bazaar's payment activity to subscribe a product.
 * Note that for purchasing a product you have to use the 'purchaseProduct' function.
 * @see purchaseProduct
 * @param fragment We use this fragment instance to actually start Bazaar's payment activity.
 * @param request This contains some information about the product that we are going to subscribe.
 * @return Completable that you can subscribe to it and it gets completed when purchase flow begins.
 */
fun Payment.subscribeProduct(fragment: Fragment, request: PurchaseRequest): Completable {
    return Completable.create { emitter ->
        subscribeProduct(fragment, request) {
            purchaseFlowBegan { emitter.onComplete() }
            failedToBeginFlow { emitter.onError(it) }
        }
    }
}
