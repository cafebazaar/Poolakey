package com.phelat.poolakey.config

/**
 * You can use this class to disable or enable local security checks for purchases and queries.
 * Note that it's highly recommended to disable local security checks and use Bazaar's REST API to
 * validate a purchase. You can check out Bazaar's documentation from here:
 * http://developers.cafebazaar.ir/fa/docs/developer-api-v2-introduction/
 * @see Disable
 * @see Enable
 */
sealed class SecurityCheck {

    /**
     * You have to use this object in order to disable local security checks.
     */
    object Disable : SecurityCheck()

    /**
     * You have to use this class in order to enable local security checks. You can access to your
     * app's public rsa key from Bazaar's developer panel, under "In-App Billing" tab:
     * https://pishkhan.cafebazaar.ir/apps/YOUR_APPS_PACKAGE_NAME/in-app-billing
     */
    data class Enable(val rsaPublicKey: String) : SecurityCheck()

}
