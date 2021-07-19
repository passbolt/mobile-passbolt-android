package com.passbolt.mobile.android.storage.encrypted.biometric

import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreWrapper(
    private val keyStore: KeyStore,
    private val keyGenerator: KeyGenerator,
    private val packageManager: PackageManager
) {

    fun getOrCreateKey(alias: String): SecretKey =
        getAndroidKeyStoreSymmetricKey(alias) ?: createAndroidKeyStoreSymmetricKey(alias)

    fun getAndroidKeyStoreSymmetricKey(alias: String): SecretKey? =
        keyStore.getKey(alias, null) as SecretKey?

    private fun createAndroidKeyStoreSymmetricKey(alias: String): SecretKey {
        val keyParamsSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setAuthTimeoutParameters()
            .setStrongBoxParameter()
            .build()

        keyGenerator.init(keyParamsSpec)
        return keyGenerator.generateKey()
    }

    private fun KeyGenParameterSpec.Builder.setAuthTimeoutParameters() = let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setUserAuthenticationParameters(KEY_AUTH_TIMEOUT_SECS, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            setUserAuthenticationValidityDurationSeconds(KEY_AUTH_TIMEOUT_SECS)
        }
    }

    private fun KeyGenParameterSpec.Builder.setStrongBoxParameter() = let {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
            setIsStrongBoxBacked(true)
        }
        it
    }

    fun removeKey(alias: String) {
        keyStore.deleteEntry(alias)
    }

    private companion object {
        private const val KEY_AUTH_TIMEOUT_SECS = 10
    }
}
