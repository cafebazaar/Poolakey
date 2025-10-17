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
 *
 * Single-file demo:
 * - Contains a corrected IInAppBillingService interface (Java-valid signatures)
 * - Provides a MockInAppBillingService with stubbed responses
 * - Provides a BillingDemoActivity with a programmatic, visually improved UI
 *
 * Note: This is a demo/stub. Replace the MockInAppBillingService with a real implementation
 * when integrating with actual billing APIs.
 */

package com.android.vending.billing;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Single-file demo activity that contains:
 * - IInAppBillingService (corrected)
 * - A MockInAppBillingService implementation (stubs)
 * - A small UI to call several billing methods and display results
 */
public class BillingDemoActivity extends AppCompatActivity {

    // ---------------------------
    // Corrected interface (Java)
    // ---------------------------
    /**
     * Java interface for in-app billing service.
     * All "in" keywords from AIDL removed; methods use standard Java types.
     */
    interface IInAppBillingService {

        int isBillingSupported(int apiVersion, String packageName, String type);

        Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle);

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
                Bundle extraData);

        Bundle checkTrialSubscription(String packageName);

        Bundle getFeatureConfig();

        // ---------------------------------------------------------------------
        // 🔹 Added functions (corrected)
        // ---------------------------------------------------------------------

        Bundle getActiveSubscriptions(int apiVersion, String packageName, String userId);

        int acknowledgePurchase(int apiVersion, String packageName, String purchaseToken);

        Bundle getSkuOffers(int apiVersion, String packageName, String sku, Bundle extraParams);

        Bundle launchPriceChangeFlow(int apiVersion, String packageName, String sku, Bundle options);

        Bundle validatePurchase(int apiVersion, String packageName, String purchaseToken, Bundle validationData);

        Bundle getPurchaseHistory(int apiVersion, String packageName, String type, String continuationToken);

        int enableDeveloperMode(int apiVersion, String packageName, boolean enabled);

        Bundle getBillingDiagnostics(int apiVersion, String packageName);

        int cancelSubscription(int apiVersion, String packageName, String sku, String userId);

        Bundle changeSubscriptionPlan(int apiVersion,
                                      String packageName,
                                      String oldSku,
                                      String newSku,
                                      Bundle options);

        Bundle getAvailablePaymentMethods(int apiVersion, String packageName);

        int prefetchSkuDetails(int apiVersion, String packageName, Bundle skuList);
    }

    // -----------------------------------
    // Mock implementation for testing
    // -----------------------------------
    /**
     * Simple mock/stub implementation of IInAppBillingService.
     * Returns sample Bundles and integer response codes.
     */
    static class MockInAppBillingService implements IInAppBillingService {

        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public int isBillingSupported(int apiVersion, String packageName, String type) {
            // 0 means OK in many billing APIs — use 0 for success
            return 0;
        }

        @Override
        public Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("DETAILS", "Sample SKU details for type=" + type + " pkg=" + packageName);
            return b;
        }

        @Override
        public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("BUY_INTENT", "intent://buy/" + sku + "?pkg=" + packageName);
            b.putString("DEVELOPER_PAYLOAD", developerPayload);
            return b;
        }

        @Override
        public Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("PURCHASES", "[{\"sku\":\"sample_monthly\",\"state\":\"active\"}]");
            b.putString("CONTINUATION_TOKEN", continuationToken == null ? "" : continuationToken);
            return b;
        }

        @Override
        public int consumePurchase(int apiVersion, String packageName, String purchaseToken) {
            // Return 0 for success
            return 0;
        }

        @Override
        public Bundle getBuyIntentV2(int apiVersion, String packageName, String sku, String type, String developerPayload) {
            // Forward to getBuyIntent for mock purposes
            return getBuyIntent(apiVersion, packageName, sku, type, developerPayload);
        }

        @Override
        public Bundle getPurchaseConfig(int apiVersion) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("CONFIG", "mock-config-v1");
            return b;
        }

        @Override
        public Bundle getBuyIntentV3(int apiVersion, String packageName, String sku, String developerPayload, Bundle extraData) {
            Bundle b = getBuyIntent(apiVersion, packageName, sku, "inapp", developerPayload);
            b.putBundle("EXTRA_DATA", extraData == null ? new Bundle() : extraData);
            return b;
        }

        @Override
        public Bundle checkTrialSubscription(String packageName) {
            Bundle b = new Bundle();
            b.putBoolean("HAS_TRIAL", true);
            b.putString("PACKAGE", packageName);
            return b;
        }

        @Override
        public Bundle getFeatureConfig() {
            Bundle b = new Bundle();
            b.putString("FEATURES", "mock-features-list");
            return b;
        }

        @Override
        public Bundle getActiveSubscriptions(int apiVersion, String packageName, String userId) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("ACTIVE_SUBSCRIPTIONS", "[{\"sku\":\"pro_monthly\",\"user\":\"" + userId + "\"}]");
            return b;
        }

        @Override
        public int acknowledgePurchase(int apiVersion, String packageName, String purchaseToken) {
            return 0; // success
        }

        @Override
        public Bundle getSkuOffers(int apiVersion, String packageName, String sku, Bundle extraParams) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("OFFERS", "[{\"offerId\":\"discount1\",\"sku\":\"" + sku + "\"}]");
            return b;
        }

        @Override
        public Bundle launchPriceChangeFlow(int apiVersion, String packageName, String sku, Bundle options) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("PRICE_CHANGE_STATUS", "price-change-flow-launched-for:" + sku);
            return b;
        }

        @Override
        public Bundle validatePurchase(int apiVersion, String packageName, String purchaseToken, Bundle validationData) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putBoolean("VALID", true);
            b.putString("PURCHASE_TOKEN", purchaseToken);
            return b;
        }

        @Override
        public Bundle getPurchaseHistory(int apiVersion, String packageName, String type, String continuationToken) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("HISTORY", "[{\"sku\":\"sample_one_time\",\"state\":\"consumed\"}]");
            return b;
        }

        @Override
        public int enableDeveloperMode(int apiVersion, String packageName, boolean enabled) {
            return enabled ? 0 : 1; // 0 success when enabling; 1 if disabled (mock)
        }

        @Override
        public Bundle getBillingDiagnostics(int apiVersion, String packageName) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("DIAGNOSTICS", "mock-diagnostics-ok");
            return b;
        }

        @Override
        public int cancelSubscription(int apiVersion, String packageName, String sku, String userId) {
            return 0; // success
        }

        @Override
        public Bundle changeSubscriptionPlan(int apiVersion, String packageName, String oldSku, String newSku, Bundle options) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("RESULT", "changed " + oldSku + " -> " + newSku);
            return b;
        }

        @Override
        public Bundle getAvailablePaymentMethods(int apiVersion, String packageName) {
            Bundle b = new Bundle();
            b.putInt("RESPONSE_CODE", 0);
            b.putString("PAYMENT_METHODS", "[\"card\",\"carrier\",\"paypal\"]");
            return b;
        }

        @Override
        public int prefetchSkuDetails(int apiVersion, String packageName, Bundle skuList) {
            // Pretend caching succeeded
            return 0;
        }
    }

    // --------------------------
    // Activity UI and behavior
    // --------------------------
    private IInAppBillingService billingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        billingService = new MockInAppBillingService();

        // Build a simple UI programmatically so everything stays in one file.
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Header
        TextView header = new TextView(this);
        header.setText("In-App Billing Demo");
        header.setTextSize(22f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, dp(12));
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setTextColor(0xFF212121);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // A short subtitle / description
        TextView subtitle = new TextView(this);
        subtitle.setText("Mock billing service — tap any action to see sample results.");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(0xFF666666);
        subtitle.setPadding(0, 0, 0, dp(12));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle);

        // Add buttons (card-like) for several actions
        root.addView(makeActionButton("Check Billing Support", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rc = billingService.isBillingSupported(3, getPackageName(), "inapp");
                showResult("isBillingSupported", "Response code: " + rc);
            }
        }));

        root.addView(makeActionButton("Get SKU Details", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle req = new Bundle();
                req.putStringArray("ITEM_ID_LIST", new String[]{"sample_one_time", "sample_monthly"});
                Bundle res = billingService.getSkuDetails(3, getPackageName(), "inapp", req);
                showBundle("getSkuDetails", res);
            }
        }));

        root.addView(makeActionButton("Get Purchases", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle res = billingService.getPurchases(3, getPackageName(), "inapp", null);
                showBundle("getPurchases", res);
            }
        }));

        root.addView(makeActionButton("Get Active Subscriptions", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle res = billingService.getActiveSubscriptions(3, getPackageName(), "user123");
                showBundle("getActiveSubscriptions", res);
            }
        }));

        root.addView(makeActionButton("Validate Purchase (mock)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle valData = new Bundle();
                valData.putString("signature", "mock-signature");
                Bundle res = billingService.validatePurchase(3, getPackageName(), "token-abc-123", valData);
                showBundle("validatePurchase", res);
            }
        }));

        // Footer note
        TextView footer = new TextView(this);
        footer.setText("\nThis UI uses a mock billing service. Replace MockInAppBillingService with\na real service implementation for production.");
        footer.setTextSize(12f);
        footer.setTextColor(0xFF888888);
        footer.setPadding(0, dp(12), 0, 0);
        footer.setMovementMethod(new ScrollingMovementMethod());
        root.addView(footer);

        scroll.addView(root);
        setContentView(scroll);
    }

    // Helper that creates a rounded, shadowless "card" button for actions.
    private View makeActionButton(String label, View.OnClickListener onClick) {
        // container with rounded background
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        container.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(0xFFF7F7F7); // light card color
        container.setBackground(bg);

        // Title
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF111111);

        // Button
        Button b = new Button(this);
        b.setText("Run");
        b.setAllCaps(false);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.gravity = Gravity.END;
        btnParams.topMargin = dp(8);
        b.setLayoutParams(btnParams);

        // Wire click: click on either card or the button triggers action
        container.setOnClickListener(onClick);
        b.setOnClickListener(onClick);

        container.addView(title);
        container.addView(b);

        return container;
    }

    // Helper: display bundle contents in an AlertDialog
    private void showBundle(String title, Bundle b) {
        StringBuilder sb = new StringBuilder();
        if (b == null) {
            sb.append("null");
        } else {
            for (String key : b.keySet()) {
                Object val = b.get(key);
                sb.append(key).append(": ").append(String.valueOf(val)).append("\n\n");
            }
        }
        showResult(title, sb.toString());
    }

    private void showResult(String title, String text) {
        // Show an AlertDialog with rounded message text
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextSize(14f);
        message.setPadding(dp(12), dp(12), dp(12), dp(12));
        message.setTextColor(0xFF222222);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private int dp(int d) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(d * scale);
    }
}
 
