package com.passbolt.mobile.android.feature.authentication.auth

import androidx.biometric.BiometricPrompt
import com.passbolt.mobile.android.feature.authentication.R

class AuthBiometricCallback(
    private val authError: (Int) -> Unit,
    private val authSucceeded: () -> Unit,
    private val authCanceled: (() -> Unit)? = null
) : BiometricPrompt.AuthenticationCallback() {

    override fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence
    ) {
        super.onAuthenticationError(errorCode, errString)
        handleError(errorCode)
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        authSucceeded.invoke()
    }

    private fun handleError(errorCode: Int) {
        when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT -> authError.invoke(R.string.fingerprint_biometric_error_blocked)
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                authError.invoke(R.string.fingerprint_biometric_error_too_many_attempts)
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_TIMEOUT -> {
                authCanceled?.invoke()
            }
            else -> authError.invoke(R.string.fingerprint_biometric_error_generic)
        }
    }
}
