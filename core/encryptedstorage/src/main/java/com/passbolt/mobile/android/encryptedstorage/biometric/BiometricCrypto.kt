package com.passbolt.mobile.android.encryptedstorage.biometric

import android.util.Base64
import javax.crypto.Cipher

class BiometricCrypto {
    fun encryptData(
        data: ByteArray,
        authenticatedCipher: Cipher,
    ): ByteArray {
        val encrypted = authenticatedCipher.doFinal(data)
        return Base64.encodeToString(encrypted, Base64.DEFAULT).toByteArray()
    }

    fun decryptData(
        data: String,
        authenticatedCipher: Cipher,
    ): ByteArray {
        val decoded = Base64.decode(data, Base64.DEFAULT)
        return authenticatedCipher.doFinal(decoded)
    }

    companion object {
        const val BIOMETRIC_KEY_ALIAS = "BIOMETRIC_KEY"
    }
}
