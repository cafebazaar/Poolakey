package ir.cafebazaar.poolakey

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.os.Build
import ir.cafebazaar.poolakey.constant.Const

/**
 * The Bazaar build this connection talks to. Resolved once per connection and reused for
 * binding, version gating and outgoing broadcasts.
 */
internal data class BazaarPackage(
    val packageName: String,
    val versionCode: Long
)

internal object BazaarPackageResolver {

    fun resolve(context: Context): BazaarPackage {
        val installedCandidates = Const.BAZAAR_PACKAGE_CANDIDATES.mapNotNull { packageName ->
            installedCandidate(context, packageName)
        }
        return installedCandidates.firstOrNull { candidate ->
            isBillingServiceAvailable(context, billingServiceIntent(candidate.packageName))
        } ?: installedCandidates.firstOrNull() ?: BazaarPackage(
            packageName = Const.BAZAAR_PACKAGE_NAME,
            versionCode = 0L,
        )
    }

    fun billingServiceIntent(packageName: String): Intent {
        return Intent(Const.BILLING_SERVICE_ACTION).apply {
            `package` = packageName
            setClassName(packageName, Const.BAZAAR_PAYMENT_SERVICE_CLASS_NAME)
        }
    }

    fun isBillingServiceAvailable(context: Context, intent: Intent): Boolean {
        return context.packageManager.queryIntentServices(intent, 0).isNotEmpty() ||
                isBillingServiceAvailableInDeepSleep(context, intent)
    }

    private fun isBillingServiceAvailableInDeepSleep(context: Context, intent: Intent): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                context.packageManager
                    .queryIntentServices(intent, MATCH_DISABLED_COMPONENTS)
                    .isNotEmpty()
    }

    private fun installedCandidate(context: Context, packageName: String): BazaarPackage? {
        val packageInfo = getPackageInfo(context, packageName) ?: return null
        val versionCode = sdkAwareVersionCode(packageInfo)
        return BazaarPackage(packageName, versionCode).takeIf {
            versionCode >= minimumVersionCode(packageName)
        }
    }

    private fun minimumVersionCode(packageName: String): Long {
        return when (packageName) {
            Const.BAZAAR_TV_PACKAGE_NAME -> Const.BAZAAR_TV_MIN_VERSION_CODE
            else -> 0L
        }
    }
}
