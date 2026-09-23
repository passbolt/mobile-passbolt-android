package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

data class AuthWithWebauthnState(
    val showProgress: Boolean = false,
    val hasOtherProvider: Boolean = false,
    val rememberMe: Boolean = true,
    val showSetupLeaveConfirmationDialog: Boolean = false,
)
