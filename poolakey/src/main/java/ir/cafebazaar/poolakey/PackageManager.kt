package ir.cafebazaar.poolakey

import android.content.Context
import android.content.pm.PackageInfo

internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    val packageManager = context.packageManager
    packageManager.getPackageInfo(packageName, 0)
} catch (ignored: Exception) {
    null
}

@Suppress("DEPRECATION")
internal fun sdkAwareVersionCode(packageInfo: PackageInfo): Long {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
}