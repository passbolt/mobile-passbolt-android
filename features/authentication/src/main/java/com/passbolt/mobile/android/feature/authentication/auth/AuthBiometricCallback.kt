package com.passbolt.mobile.android.feature.authentication.auth

import androidx.biometric.BiometricPrompt

class AuthBiometricCallback(
    private val authError: (Int) -> Unit,
    private val authSucceeded: () -> Unit
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
        // TODO
    }
}
