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

    // New methods to extend functionality

    /**
     * Retrieves the subscription status for a given SKU and package.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param sku The SKU of the subscription to check.
     * @return A Bundle containing subscription status details (e.g., active, expired, cancelled).
     */
    Bundle getSubscriptionStatus(int apiVersion, String packageName, String sku);

    /**
     * Initiates a request to cancel an active subscription.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param purchaseToken The token of the subscription purchase to cancel.
     * @return An integer indicating the result (e.g., 0 for success, error code otherwise).
     */
    int cancelSubscription(int apiVersion, String packageName, String purchaseToken);

    /**
     * Retrieves available promotional offers for a given SKU.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param sku The SKU to check for promotional offers.
     * @return A Bundle containing details of available promotions (e.g., discount percentage, duration).
     */
    Bundle getPromotionalOffers(int apiVersion, String packageName, String sku);

    /**
     * Initiates a refund request for a purchase.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param purchaseToken The token of the purchase to refund.
     * @return A Bundle containing the refund request status (e.g., approved, pending, denied).
     */
    Bundle requestRefund(int apiVersion, String packageName, String purchaseToken);

    /**
     * Retrieves the purchase history for a specific user across devices.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param type The type of purchase ("inapp" or "subs").
     * @param continuationToken A token for paginated results.
     * @return A Bundle containing the user’s purchase history.
     */
    Bundle getPurchaseHistory(int apiVersion, String packageName, String type, String continuationToken);

    /**
     * Validates a purchase token to ensure it is still valid.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param purchaseToken The token of the purchase to validate.
     * @return A Bundle containing validation status (e.g., valid, invalid, expired).
     */
    Bundle validatePurchase(int apiVersion, String packageName, String purchaseToken);

    /**
     * Applies a promotional code to a purchase intent.
     * @param apiVersion The billing API version.
     * @param packageName The package name of the app.
     * @param sku The SKU to purchase.
     * @param type The type of purchase ("inapp" or "subs").
     * @param promoCode The promotional code to apply.
     * @param developerPayload Additional payload for the purchase.
     * @return A Bundle containing the modified buy intent with the applied promotion.
     */
    Bundle getBuyIntentWithPromo(int apiVersion,
        String packageName,
        String sku,
        String type,
        String promoCode,
        String developerPayload);
} 
