package com.phelat.poolakey.security

import android.util.Base64
import java.lang.IllegalArgumentException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

internal class PurchaseVerifier {

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IllegalArgumentException::class
    )
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        val key = generatePublicKey(base64PublicKey)
        return verify(key, signedData, signature)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        IllegalArgumentException::class
    )
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
        signatureAlgorithm.initVerify(publicKey)
        signatureAlgorithm.update(signedData.toByteArray())
        return signatureAlgorithm.verify(Base64.decode(signature, Base64.DEFAULT))
    }

    companion object {
        private const val KEY_FACTORY_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    }

}
