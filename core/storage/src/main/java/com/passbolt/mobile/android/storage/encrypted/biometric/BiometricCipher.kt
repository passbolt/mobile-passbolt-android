package com.passbolt.mobile.android.storage.encrypted.biometric

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCrypto.Companion.BIOMETRIC_KEY_ALIAS
import com.passbolt.mobile.android.storage.usecase.biometrickey.GetBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

class BiometricCipher(
    private val keyStoreWrapper: KeyStoreWrapper,
    private val getBiometricKeyIvUseCase: GetBiometricKeyIvUseCase
) {

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun getBiometricEncryptCipher(): Cipher = newSymmetricCipher().apply {
        val biometricKey = keyStoreWrapper.getOrCreateSymmetricKey(BIOMETRIC_KEY_ALIAS)
        init(Cipher.ENCRYPT_MODE, biometricKey)
    }

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun getBiometricDecryptCipher(userId: String): Cipher = newSymmetricCipher().apply {
        val key = keyStoreWrapper.getSymmetricKey(BIOMETRIC_KEY_ALIAS)
            ?: throw SecurityException("Unable to decrypt: No keys found")
        val ivOutput = getBiometricKeyIvUseCase.execute(UserIdInput(userId))
        init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ivOutput.iv))
    }

    companion object {
        private const val TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"

        private fun newSymmetricCipher() =
            Cipher.getInstance(TRANSFORMATION_SYMMETRIC)
    }
}
