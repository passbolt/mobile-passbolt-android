package com.passbolt.mobile.android.feature.authentication.auth

import androidx.biometric.BiometricPrompt
import javax.crypto.Cipher
import com.passbolt.mobile.android.core.localization.R as LocalizationR

class AuthBiometricCallback(
    private val authError: (Int) -> Unit,
    private val authSucceeded: (Cipher?) -> Unit,
    private val authCanceled: (() -> Unit)? = null,
) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence,
    ) {
        super.onAuthenticationError(errorCode, errString)
        handleError(errorCode)
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        authSucceeded.invoke(result.cryptoObject?.cipher)
    }

    private fun handleError(errorCode: Int) {
        when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT -> authError.invoke(LocalizationR.string.fingerprint_biometric_error_blocked)
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                authError.invoke(LocalizationR.string.fingerprint_biometric_error_too_many_attempts)
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_TIMEOUT,
            -> {
                authCanceled?.invoke()
            }
            else -> authError.invoke(LocalizationR.string.fingerprint_biometric_error_generic)
        }
    }
}
