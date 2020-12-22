package ir.cafebazaar.poolakey

import android.content.Context
import android.content.pm.PackageInfo

internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    val packageManager = context.packageManager
    packageManager.getPackageInfo(packageName, 0)
} catch (ignored: Exception) {
    null
}