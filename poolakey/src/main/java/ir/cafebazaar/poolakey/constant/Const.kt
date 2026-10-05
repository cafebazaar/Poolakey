package ir.cafebazaar.poolakey.constant

internal object Const {
    const val BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
    const val BAZAAR_TV_PACKAGE_NAME = "com.farsitel.bazaar.tv"

    /**
     * Bazaar application ids that expose the in-app billing components.
     */
    val BAZAAR_PACKAGE_CANDIDATES = listOf(BAZAAR_PACKAGE_NAME, BAZAAR_TV_PACKAGE_NAME)

    /**
     * The TV-only build shares the phone build's version stream and first shipped as 30.0.0.
     */
    const val BAZAAR_TV_MIN_VERSION_CODE = 3000000L

    /**
     * Component class names are identical in every Bazaar build, only the application id differs.
     */
    const val BAZAAR_PAYMENT_SERVICE_CLASS_NAME =
        "com.farsitel.bazaar.inappbilling.service.InAppBillingService"
    const val BILLING_SERVICE_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
}
