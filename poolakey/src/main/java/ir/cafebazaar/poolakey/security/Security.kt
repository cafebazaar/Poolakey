package ir.cafebazaar.poolakey.security

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import ir.cafebazaar.poolakey.BuildConfig
import ir.cafebazaar.poolakey.constant.Const.BAZAAR_PACKAGE_NAME
import ir.cafebazaar.poolakey.getPackageInfo
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

/**
 * Security utilities for Poolakey — verifies Bazaar installation & certificates,
 * provides diagnostics and some lightweight UI helpers for displaying reports.
 *
 * All functions are `internal` so they remain library-internal.
 */
internal object Security {

    private const val TAG = "PoolakeySecurity"

    // =======================================================
    // ✅ EXISTING CORE VERIFICATION
    // =======================================================

    /**
     * Verifies Bazaar is installed and its certificate public-key hex matches
     * the expected hash from BuildConfig.BAZAAR_HASH.
     */
    fun verifyBazaarIsInstalled(context: Context): Boolean {
        val packageInfo = getPackageInfo(context, BAZAAR_PACKAGE_NAME)
            ?: return false.also { Log.w(TAG, "Bazaar not installed.") }

        val signatures = getSignaturesSafe(context, BAZAAR_PACKAGE_NAME)
        if (signatures.isEmpty()) {
            Log.e(TAG, "No signatures found for Bazaar package.")
            return false
        }

        for (signature in signatures) {
            val certificate = signatureToX509(signature) ?: continue
            val publicKey: PublicKey = certificate.publicKey
            val certificateHex = byte2HexFormatted(publicKey.encoded)
            if (BuildConfig.BAZAAR_HASH == certificateHex) {
                Log.i(TAG, "✅ Bazaar verification successful.")
                return true
            }
        }

        Log.w(TAG, "❌ Bazaar signature mismatch detected.")
        return false
    }

    // =======================================================
    // 🆕 ADDITIONAL SECURITY & DIAGNOSTIC FUNCTIONS
    // =======================================================

    /**
     * Returns Bazaar app version info (for logging or UI display).
     */
    fun getBazaarVersionInfo(context: Context): String? {
        val packageInfo = getPackageInfo(context, BAZAAR_PACKAGE_NAME) ?: return null
        // Use longVersionCode on newer SDKs if available, else versionCode
        val versionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode
            else packageInfo.versionCode.toLong()
        } catch (e: Exception) {
            -1L
        }
        return "Bazaar ${packageInfo.versionName} (code: $versionCode)"
    }

    /**
     * Verifies Bazaar's certificate SHA-256 fingerprint equals expectedFingerprint.
     * Fingerprint format should be hex pairs separated by ':' (uppercase or lowercase accepted).
     */
    fun verifyBazaarCertificateSHA256(context: Context, expectedFingerprint: String): Boolean {
        val signatures = getSignaturesSafe(context, BAZAAR_PACKAGE_NAME)
        if (signatures.isEmpty()) {
            Log.e(TAG, "No signatures available for SHA-256 check.")
            return false
        }
        val normalizedExpected = expectedFingerprint.replace("\\s".toRegex(), "").uppercase(Locale.US)
        for (signature in signatures) {
            val cert = signatureToX509(signature) ?: continue
            val fingerprint = getCertificateSHA256Fingerprint(cert).replace(":", "")
            if (fingerprint.equals(normalizedExpected, ignoreCase = true) ||
                getCertificateSHA256Fingerprint(cert).equals(expectedFingerprint, ignoreCase = true)
            ) {
                Log.i(TAG, "✅ Bazaar certificate SHA-256 verified successfully.")
                return true
            }
        }
        Log.e(TAG, "❌ Bazaar certificate SHA-256 verification failed.")
        return false
    }

    /**
     * Extracts SHA-256 fingerprint of a certificate, formatted as 'AA:BB:CC...'.
     */
    private fun getCertificateSHA256Fingerprint(certificate: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificate.encoded)
        return hash.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Generates a short security report about Bazaar installation & signatures.
     */
    fun generateSecurityReport(context: Context): String {
        val packageInfo = getPackageInfo(context, BAZAAR_PACKAGE_NAME)
        val installed = packageInfo != null
        val version = packageInfo?.versionName ?: "N/A"
        val signatures = if (installed) getSignaturesSafe(context, BAZAAR_PACKAGE_NAME).size else 0

        return buildString {
            appendLine("🔍 Poolakey Security Report")
            appendLine("===================================")
            appendLine("📦 Bazaar Installed: $installed")
            appendLine("🏷️  Version: $version")
            appendLine("🔏 Signature Count: $signatures")
            appendLine("🕒 Generated: ${Date()}")
            appendLine("===================================")
        }
    }

    /**
     * Safe wrapper to get signatures — returns empty array instead of throwing.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    private fun getSignaturesSafe(context: Context, packageName: String): Array<Signature> {
        val packageManager = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo.apkContentsSigners
            } else {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                packageInfo.signatures
            }
        } catch (e: Exception) {
            Log.w(TAG, "getSignaturesSafe: unable to fetch signatures for $packageName: ${e.message}")
            emptyArray()
        }
    }

    /**
     * Converts a Signature to X509Certificate, or null on error.
     */
    private fun signatureToX509(signature: Signature): X509Certificate? {
        return try {
            val input: InputStream = ByteArrayInputStream(signature.toByteArray())
            val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X509")
            certificateFactory.generateCertificate(input) as X509Certificate
        } catch (e: Exception) {
            Log.w(TAG, "signatureToX509: failed to convert signature to certificate: ${e.message}")
            null
        }
    }

    /**
     * Converts a byte array into a formatted hex string (used for certificate comparison).
     * Uses unsigned byte handling to avoid negative hex values.
     */
    private fun byte2HexFormatted(array: ByteArray): String {
        val stringBuilder = StringBuilder(array.size * 3) // include ':' separators
        for (index in array.indices) {
            val unsigned = array[index].toInt() and 0xFF
            val suggestedHex = Integer.toHexString(unsigned)
            if (suggestedHex.length == 1) {
                stringBuilder.append('0')
            }
            stringBuilder.append(suggestedHex.uppercase(Locale.US))
            if (index < array.size - 1) {
                stringBuilder.append(':')
            }
        }
        return stringBuilder.toString()
    }

    /**
     * Prints the security report to logcat.
     */
    fun printSecuritySummary(context: Context) {
        val report = generateSecurityReport(context)
        Log.i(TAG, report)
    }

    // =======================================================
    // 🧩 NEWLY ADDED UTILITY FUNCTIONS
    // =======================================================

    /**
     * Detects common indicators of a rooted device by checking for known su locations.
     * Note: This is heuristic and not foolproof.
     */
    fun isDeviceRooted(): Boolean {
        val dangerousPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return dangerousPaths.any { path -> java.io.File(path).exists() }
    }

    /**
     * Returns true if the current app is debuggable.
     */
    fun isAppDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Returns app's own SHA-256 certificate fingerprint (first signature) or null.
     */
    fun getOwnAppSignatureFingerprint(context: Context): String? {
        val signatures = getSignaturesSafe(context, context.packageName)
        if (signatures.isEmpty()) return null
        val cert = signatureToX509(signatures[0]) ?: return null
        return getCertificateSHA256Fingerprint(cert)
    }

    /**
     * Compares the app's first signature fingerprint with Bazaar's first signature fingerprint.
     * Useful when linking trust between app and Bazaar (optional).
     */
    fun compareAppWithBazaarSignature(context: Context): Boolean {
        val bazaarSignatures = getSignaturesSafe(context, BAZAAR_PACKAGE_NAME)
        val appSignatures = getSignaturesSafe(context, context.packageName)
        if (bazaarSignatures.isEmpty() || appSignatures.isEmpty()) return false

        val bazaarCert = signatureToX509(bazaarSignatures[0]) ?: return false
        val appCert = signatureToX509(appSignatures[0]) ?: return false

        return getCertificateSHA256Fingerprint(bazaarCert) == getCertificateSHA256Fingerprint(appCert)
    }

    /**
     * Generates full integrity report: Bazaar verification + app fingerprint + device state.
     */
    fun generateFullIntegrityReport(context: Context): String {
        val bazaarVerified = verifyBazaarIsInstalled(context)
        val rooted = isDeviceRooted()
        val debuggable = isAppDebuggable(context)
        val appFingerprint = getOwnAppSignatureFingerprint(context) ?: "N/A"

        return buildString {
            appendLine("🧭 Full Integrity Report")
            appendLine("===================================")
            appendLine("📦 Bazaar Verified: $bazaarVerified")
            appendLine("🔐 App Debuggable: $debuggable")
            appendLine("⚠️  Device Rooted: $rooted")
            appendLine("🔏 App Fingerprint (SHA-256): $appFingerprint")
            appendLine("🕒 Checked at: ${Date()}")
            appendLine("===================================")
        }
    }

    // =======================================================
    // 🎨 Lightweight UI Helpers (visually improved display)
    // =======================================================

    /**
     * Shows a short Toast with a security summary (not blocking).
     */
    fun showSecurityToast(context: Context) {
        val bazaarOk = verifyBazaarIsInstalled(context)
        val msg = if (bazaarOk) "Bazaar verified ✅" else "Bazaar verification failed ❌"
        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Binds a generated full integrity report into a TextView with a pleasing style.
     * The TextView will be styled as a rounded card with gradient depending on verification state.
     */
    fun bindReportToTextView(context: Context, textView: TextView) {
        val report = generateFullIntegrityReport(context)
        textView.text = report
        textView.typeface = Typeface.MONOSPACE
        textView.textSize = 12f
        textView.setTextColor(Color.WHITE)
        textView.setPadding(24, 24, 24, 24)

        val bazaarOk = verifyBazaarIsInstalled(context)
        val startColor = if (bazaarOk) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        val endColor = if (bazaarOk) Color.parseColor("#388E3C") else Color.parseColor("#D32F2F")

        val drawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = 16f * context.resources.displayMetrics.density
        }

        textView.background = drawable
        textView.elevation = 8f
        textView.visibility = View.VISIBLE
    }

    /**
     * Copies the current full integrity report to clipboard and returns whether succeeded.
     */
    fun copyReportToClipboard(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val report = generateFullIntegrityReport(context)
            val clip = ClipData.newPlainText("Poolakey Security Report", report)
            cm.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            Log.w(TAG, "copyReportToClipboard failed: ${e.message}")
            false
        }
    }
} 
