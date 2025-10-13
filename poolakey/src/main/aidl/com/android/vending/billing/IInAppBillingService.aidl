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
} 
