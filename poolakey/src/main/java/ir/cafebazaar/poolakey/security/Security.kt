package ir.cafebazaar.poolakey.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import ir.cafebazaar.poolakey.BuildConfig
import ir.cafebazaar.poolakey.constant.Const.BAZAAR_PACKAGE_NAME
import ir.cafebazaar.poolakey.getPackageInfo
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

internal object Security {

    fun verifyBazaarIsInstalled(context: Context): Boolean {

        if (getPackageInfo(context, BAZAAR_PACKAGE_NAME) == null) {
            return false
        }

        val packageManager: PackageManager = context.packageManager

        @Suppress("DEPRECATION")
        @SuppressLint("PackageManagerGetSignatures")
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = packageManager.getPackageInfo(
                BAZAAR_PACKAGE_NAME,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            packageInfo.signingInfo.apkContentsSigners
        } else {
            val packageInfo = packageManager.getPackageInfo(
                BAZAAR_PACKAGE_NAME,
                PackageManager.GET_SIGNATURES
            )
            packageInfo.signatures
        }

        var certificateMatch = true
        for (sig in signatures) {
            val input: InputStream = ByteArrayInputStream(sig.toByteArray())
            val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X509")
            val certificate: X509Certificate =
                certificateFactory.generateCertificate(input) as X509Certificate
            val publicKey: PublicKey = certificate.publicKey
            val certificateHex = byte2HexFormatted(publicKey.encoded)
            if (BuildConfig.BAZAAR_HASH != certificateHex) {
                certificateMatch = false
                break
            }
        }

        return certificateMatch
    }

    private fun byte2HexFormatted(array: ByteArray): String {
        val stringBuilder = StringBuilder(array.size * 2)
        for (index in array.indices) {
            var suggestedHex = Integer.toHexString(array[index].toInt())
            val length = suggestedHex.length
            if (length == 1) {
                suggestedHex = "0$suggestedHex"
            } else if (length > 2) {
                suggestedHex = suggestedHex.substring(length - 2, length)
            }
            stringBuilder.append(suggestedHex.toUpperCase(Locale.getDefault()))
            if (index < array.size - 1) {
                stringBuilder.append(':')
            }
        }
        return stringBuilder.toString()
    }
}