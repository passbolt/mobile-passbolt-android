package com.passbolt.mobile.android.storage.encrypted.biometric

import android.security.keystore.UserNotAuthenticatedException
import javax.crypto.Cipher

class Crypto(
    private val keyStoreWrapper: KeyStoreWrapper
) {

    @Throws(UserNotAuthenticatedException::class)
    fun encryptData(data: ByteArray): ByteArray {
        val key = keyStoreWrapper.getOrCreateKey(MASTER_KEY_ALIAS)
        return CipherWrapper(getSymmetricCipher())
            .encrypt(data, key)
    }

    @Throws(UserNotAuthenticatedException::class)
    fun decryptData(data: String): ByteArray {
        val key = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY_ALIAS)
            ?: throw SecurityException("Unable to decrypt: Key $MASTER_KEY_ALIAS not found")

        return CipherWrapper(getSymmetricCipher())
            .decrypt(data, key)
    }

    fun removeKey() {
        keyStoreWrapper.removeKey(MASTER_KEY_ALIAS)
    }

    companion object {
        private const val MASTER_KEY_ALIAS = "BIOMETRIC_KEY"
        private const val TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"

        private fun getSymmetricCipher(): Cipher = Cipher.getInstance(TRANSFORMATION_SYMMETRIC)
    }
}
