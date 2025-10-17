package ir.cafebazaar.poolakey

import android.content.Intent
import ir.cafebazaar.poolakey.callback.PurchaseCallback
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.constant.BazaarIntent
import ir.cafebazaar.poolakey.exception.PurchaseHijackedException
import ir.cafebazaar.poolakey.mapper.RawDataToPurchaseInfo
import ir.cafebazaar.poolakey.security.PurchaseVerifier
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException

internal class PurchaseResultParser(
    private val rawDataToPurchaseInfo: RawDataToPurchaseInfo,
    private val purchaseVerifier: PurchaseVerifier
) {

    // ANSI color codes for terminal logs
    private val RESET = "\u001B[0m"
    private val GREEN = "\u001B[32m"
    private val RED = "\u001B[31m"
    private val YELLOW = "\u001B[33m"
    private val CYAN = "\u001B[36m"
    private val BOLD = "\u001B[1m"

    fun handleReceivedResult(
        securityCheck: SecurityCheck,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        val code = data?.extras?.get(BazaarIntent.RESPONSE_CODE)
        if (code == BazaarIntent.RESPONSE_RESULT_OK) {
            parseResult(securityCheck, data, purchaseCallback)
        } else {
            logError("Response code invalid: $code")
            PurchaseCallback().apply(purchaseCallback)
                .purchaseFailed
                .invoke(IllegalStateException("Response code is not valid"))
        }
    }

    private fun parseResult(
        securityCheck: SecurityCheck,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        val purchaseData = data?.getStringExtra(BazaarIntent.RESPONSE_PURCHASE_DATA)
        val dataSignature = data?.getStringExtra(BazaarIntent.RESPONSE_SIGNATURE_DATA)

        if (purchaseData != null && dataSignature != null) {
            validatePurchase(
                securityCheck = securityCheck,
                purchaseData = purchaseData,
                dataSignature = dataSignature,
                purchaseIsValid = {
                    val purchaseInfo = rawDataToPurchaseInfo.mapToPurchaseInfo(
                        purchaseData,
                        dataSignature
                    )
                    logSuccess("Purchase validated successfully: $purchaseInfo")
                    PurchaseCallback().apply(purchaseCallback)
                        .purchaseSucceed
                        .invoke(purchaseInfo)
                },
                purchaseIsNotValid = { throwable ->
                    logError("Purchase validation failed: ${throwable.message}")
                    PurchaseCallback().apply(purchaseCallback)
                        .purchaseFailed
                        .invoke(throwable)
                }
            )
        } else {
            logError("Received invalid purchase data or signature")
            PurchaseCallback().apply(purchaseCallback)
                .purchaseFailed
                .invoke(IllegalStateException("Received data is not valid"))
        }
    }

    private inline fun validatePurchase(
        securityCheck: SecurityCheck,
        purchaseData: String,
        dataSignature: String,
        purchaseIsValid: () -> Unit,
        purchaseIsNotValid: (Throwable) -> Unit
    ) {
        if (securityCheck is SecurityCheck.Enable) {
            try {
                val isPurchaseValid = purchaseVerifier.verifyPurchase(
                    securityCheck.rsaPublicKey,
                    purchaseData,
                    dataSignature
                )
                if (isPurchaseValid) {
                    purchaseIsValid.invoke()
                } else {
                    purchaseIsNotValid.invoke(PurchaseHijackedException())
                }
            } catch (e: Exception) {
                purchaseIsNotValid.invoke(e)
            }
        } else {
            purchaseIsValid.invoke()
        }
    }

    // -------------------- Additional Helper Functions --------------------

    fun parsePurchaseSafe(data: Intent?): Result<Any> = try {
        val purchaseData = data?.getStringExtra(BazaarIntent.RESPONSE_PURCHASE_DATA)
        val signature = data?.getStringExtra(BazaarIntent.RESPONSE_SIGNATURE_DATA)
        if (purchaseData != null && signature != null) {
            val info = rawDataToPurchaseInfo.mapToPurchaseInfo(purchaseData, signature)
            logSuccess("Parsed purchase info: $info")
            Result.success(info)
        } else {
            logError("Invalid purchase data")
            Result.failure(IllegalStateException("Invalid purchase data"))
        }
    } catch (e: Exception) {
        logError("Exception parsing purchase: ${e.message}")
        Result.failure(e)
    }

    fun logPurchaseInfo(data: Intent?) {
        parsePurchaseSafe(data).onSuccess { info ->
            println("$GREEN✅ Purchase info: $info$RESET")
        }.onFailure { throwable ->
            println("$RED❌ Failed to parse purchase info: ${throwable.message}$RESET")
        }
    }

    fun handleWithRetry(
        securityCheck: SecurityCheck,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit,
        maxRetries: Int = 3
    ) {
        var attempts = 0
        fun tryValidate() {
            attempts++
            handleReceivedResult(securityCheck, data) {
                this.purchaseFailed = { throwable ->
                    if (attempts < maxRetries) {
                        println("$YELLOW⚠️ Retry attempt $attempts due to: ${throwable.message}$RESET")
                        tryValidate()
                    } else {
                        purchaseCallback()
                    }
                }
                this.purchaseSucceed = purchaseCallback().purchaseSucceed
            }
        }
        tryValidate()
    }

    fun handleMultiplePurchases(
        securityCheck: SecurityCheck,
        dataList: List<Intent?>,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        dataList.forEach { intent ->
            handleReceivedResult(securityCheck, intent, purchaseCallback)
        }
    }

    fun isPurchaseRevoked(data: Intent?): Boolean {
        val status = data?.extras?.getInt(BazaarIntent.RESPONSE_PURCHASE_STATE, -1)
        val revoked = status == BazaarIntent.PURCHASE_STATE_CANCELED
        if (revoked) logError("Purchase has been revoked or canceled")
        return revoked
    }

    // -------------------- Logging Helpers --------------------

    private fun logError(message: String) {
        println("$RED$BOLD❌ ERROR: $message$RESET")
    }

    private fun logSuccess(message: String) {
        println("$GREEN$BOLD✅ SUCCESS: $message$RESET")
    }

    private fun logInfo(message: String) {
        println("$CYANℹ️ INFO: $message$RESET")
    }
}
 
