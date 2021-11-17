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
}
