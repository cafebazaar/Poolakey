package ir.cafebazaar.poolakey

import android.content.Context

internal fun getPackageInfo(context: Context, packageName: String) = try {
    val packageManager = context.packageManager
    packageManager.getPackageInfo(packageName, 0)
} catch (ignored: Exception) {
    null
}