/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingService {
    
    int isBillingSupported(int apiVersion, String packageName, String type);

    Bundle getSkuDetails(int apiVersion, String packageName, String type, in Bundle skusBundle);

    Bundle getBuyIntent(int apiVersion,
        String packageName,
        String sku,
        String type,
        String developerPayload);

    Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken);

    int consumePurchase(int apiVersion, String packageName, String purchaseToken);

    Bundle getBuyIntentV2(int apiVersion,
        String packageName,
        String sku,
        String type,
        String developerPayload);

    Bundle getPurchaseConfig(int apiVersion);

    Bundle getBuyIntentV3(
        int apiVersion,
        String packageName,
        String sku,
        String developerPayload,
        in Bundle extraData);

    Bundle checkTrialSubscription(String packageName);

    Bundle getFeatureConfig();

    /**
     * Cancels an active subscription for the specified package and SKU.
     * Returns a Bundle containing the result code and cancellation details.
     */
    Bundle cancelSubscription(int apiVersion, String packageName, String sku, String purchaseToken);

    /**
     * Retrieves the purchase history for a specific user, including past purchases and their states.
     * Returns a Bundle containing a list of purchase details.
     */
    Bundle getPurchaseHistory(int apiVersion, String packageName, String type, String continuationToken);

    /**
     * Validates a purchase token to confirm its authenticity with the billing service.
     * Returns an int indicating validation status (0 for valid, error codes otherwise).
     */
    int validatePurchase(int apiVersion, String packageName, String purchaseToken);

    /**
     * Initiates a refund request for a specific purchase.
     * Returns a Bundle containing the result code and refund status.
     */
    Bundle requestRefund(int apiVersion, String packageName, String purchaseToken);

    /**
     * Retrieves promotional offers or discounts available for a specific SKU.
     * Returns a Bundle containing promotional details, such as discount percentage or free trial periods.
     */
    Bundle getPromotionalOffers(int apiVersion, String packageName, String sku, String type);

    /**
     * Upgrades or downgrades an existing subscription to a new SKU.
     * Returns a Bundle containing the result code and details of the new subscription.
     */
    Bundle upgradeSubscription(int apiVersion, String packageName, String oldSku, String newSku, String purchaseToken);

    /**
     * Retrieves the current billing status for a user, including active subscriptions and pending purchases.
     * Returns a Bundle containing status details.
     */
    Bundle getBillingStatus(int apiVersion, String packageName, String type);

    /**
     * Pauses an active subscription, if supported by the SKU.
     * Returns a Bundle containing the result code and pause status.
     */
    Bundle pauseSubscription(int apiVersion, String packageName, String sku, String purchaseToken);

    /**
     * Applies a promotional code to a purchase or subscription.
     * Returns a Bundle containing the result code and details of the applied promotion.
     */
    Bundle applyPromoCode(int apiVersion, String packageName, String sku, String promoCode);

    /**
     * Retrieves the payment methods available for the user.
     * Returns a Bundle containing a list of supported payment methods.
     */
    Bundle getAvailablePaymentMethods(int apiVersion, String packageName);

    /**
     * Resumes a paused subscription, if supported by the SKU.
     * Returns a Bundle containing the result code and resume status.
     */
    Bundle resumeSubscription(int apiVersion, String packageName, String sku, String purchaseToken);

    /**
     * Retrieves the billing account details for a user, such as linked account IDs.
     * Returns a Bundle containing the result code and account details.
     */
    Bundle getBillingAccountDetails(int apiVersion, String packageName);

    /**
     * Initiates a purchase flow with a specific payment method.
     * Returns a Bundle containing the result code and a PendingIntent for the purchase.
     */
    Bundle getBuyIntentWithPaymentMethod(int apiVersion, String packageName, String sku, String type, String developerPayload, String paymentMethodId, in Bundle extraData);

    /**
     * Checks the eligibility of a user for a specific SKU (e.g., based on region or account status).
     * Returns a Bundle containing the result code and eligibility details.
     */
    Bundle checkSkuEligibility(int apiVersion, String packageName, String sku, String type);

    /**
     * Retrieves subscription renewal details, such as next billing date or auto-renew status.
     * Returns a Bundle containing the result code and renewal details.
     */
    Bundle getSubscriptionRenewalDetails(int apiVersion, String packageName, String sku, String purchaseToken);
}

package ir.cafebazaar.poolakey.rxbase.exception;

import java.lang.Exception;

/**
 * Exception thrown when a purchase is canceled by the user.
 */
class PurchaseCanceledException : Exception() {
    override val message: String
        get() = "Purchase canceled by user"
}

/**
 * Exception thrown when a purchase fails due to an invalid SKU.
 */
class InvalidSkuException : Exception() {
    override val message: String
        get() = "The specified SKU is invalid or not found"
}

/**
 * Exception thrown when a purchase fails due to an unsupported payment method.
 */
class UnsupportedPaymentMethodException : Exception() {
    override val message: String
        get() = "The selected payment method is not supported"
}

/**
 * Exception thrown when a subscription operation (e.g., pause, resume) is not supported for the SKU.
 */
class SubscriptionOperationNotSupportedException : Exception() {
    override val message: String
        get() = "The requested subscription operation is not supported for this SKU"
}
