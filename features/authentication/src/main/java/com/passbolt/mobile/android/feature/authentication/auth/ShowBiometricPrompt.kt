package com.passbolt.mobile.android.feature.authentication.auth

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.passbolt.mobile.android.ui.BiometricAuthError
import timber.log.Timber
import java.util.concurrent.Executor
import javax.crypto.Cipher
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Suppress("LongParameterList")
fun showBiometricPrompt(
    activity: AppCompatActivity,
    executor: Executor,
    biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder,
    fingerprintEncryptionCipher: Cipher,
    title: String = activity.getString(LocalizationR.string.settings_turn_on_biometric_title),
    subtitle: String = activity.getString(LocalizationR.string.settings_turn_on_biometric_subtitle),
    onAuthenticationSuccess: (Cipher?) -> Unit,
    onAuthenticationError: (BiometricAuthError) -> Unit,
    onAuthenticationCancelled: () -> Unit,
    onKeyPermanentlyInvalidated: (KeyPermanentlyInvalidatedException) -> Unit,
) {
    try {
        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_LOCKOUT -> onAuthenticationError(BiometricAuthError.ERROR_LOCKOUT)
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> onAuthenticationError(BiometricAuthError.ERROR_LOCKOUT_PERMANENT)
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_TIMEOUT,
                            -> onAuthenticationCancelled()
                            else -> onAuthenticationError(BiometricAuthError.GENERIC)
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onAuthenticationSuccess(result.cryptoObject?.cipher)
                    }
                },
            )

        val promptInfo =
            biometricPromptBuilder
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(activity.getString(LocalizationR.string.cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingerprintEncryptionCipher))
    } catch (e: KeyPermanentlyInvalidatedException) {
        onKeyPermanentlyInvalidated(e)
    } catch (e: Exception) {
        Timber.e(e, "Error showing biometric prompt")
        onAuthenticationError(BiometricAuthError.GENERIC)
    }
}
