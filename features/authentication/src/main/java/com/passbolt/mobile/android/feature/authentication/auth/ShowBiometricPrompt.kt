package com.passbolt.mobile.android.feature.authentication.auth

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.passbolt.mobile.android.ui.BiometricAuthError
import java.util.concurrent.Executor
import javax.crypto.Cipher
import com.passbolt.mobile.android.core.localization.R as LocalizationR

fun showBiometricPrompt(
    activity: AppCompatActivity,
    executor: Executor,
    biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder,
    fingerprintEncryptionCipher: Cipher,
    onAuthenticationSuccess: (Cipher?) -> Unit,
    onAuthenticationError: (BiometricAuthError) -> Unit,
    onAuthenticationCancelled: () -> Unit,
) {
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
                        -> {
                            onAuthenticationCancelled()
                        }
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
            .setTitle(activity.getString(LocalizationR.string.settings_turn_on_biometric_title))
            .setSubtitle(activity.getString(LocalizationR.string.settings_turn_on_biometric_subtitle))
            .setNegativeButtonText(activity.getString(LocalizationR.string.cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingerprintEncryptionCipher))
}
