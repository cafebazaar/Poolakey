package ir.cafebazaar.poolakey.config

import android.util.Base64
import junit.framework.TestCase
import java.security.KeyPairGenerator

class SecurityCheckTest : TestCase() {

    fun testRejectsEmptyPublicKey() {
        for (key in listOf("", " ", "\t\n")) {
            assertInvalidPublicKey(key)
        }
    }

    fun testRejectsMalformedPublicKey() {
        for (key in listOf("not-a-public-key", "%%%", "AQID")) {
            assertInvalidPublicKey(key)
        }
    }

    fun testAcceptsValidRsaPublicKey() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(1024)
        }.generateKeyPair()
        val publicKey = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)

        assertEquals(publicKey, SecurityCheck.Enable(publicKey).rsaPublicKey)
    }

    private fun assertInvalidPublicKey(publicKey: String) {
        try {
            SecurityCheck.Enable(publicKey)
            fail("Invalid RSA public key was accepted")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Invalid RSA public key", exception.message)
            assertNotNull(exception.cause)
        }
    }
}
