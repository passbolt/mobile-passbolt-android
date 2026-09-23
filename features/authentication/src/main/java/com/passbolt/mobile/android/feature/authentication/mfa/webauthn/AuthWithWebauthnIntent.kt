package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

sealed interface AuthWithWebauthnIntent {
    data object AuthenticateWithWebauthn : AuthWithWebauthnIntent

    data class ToggleRememberMe(
        val checked: Boolean,
    ) : AuthWithWebauthnIntent

    data object ChooseOtherProvider : AuthWithWebauthnIntent

    data object Close : AuthWithWebauthnIntent

    data object ConfirmSetupLeave : AuthWithWebauthnIntent

    data object DismissSetupLeave : AuthWithWebauthnIntent
}
