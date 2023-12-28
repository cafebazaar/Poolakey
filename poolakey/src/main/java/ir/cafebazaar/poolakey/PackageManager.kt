package ir.cafebazaar.poolakey

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

internal fun getPackageInfo(context: Context, packageName: String): PackageInfo? = try {
    val packageManager = context.packageManager
    packageManager.getPackageInfo(packageName, 0)
} catch (ignored: Exception) {
    null
}

@Suppress("DEPRECATION")
internal fun sdkAwareVersionCode(packageInfo: PackageInfo): Long {
    return if (isSdkPieAndUp()) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
}

internal fun isSdkNougatAndUp() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

internal fun isSdkPieAndUp() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P