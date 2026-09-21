package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

sealed interface AuthWithWebauthnSideEffect {
    data class NotifyOtherProviderClicked(
        val bearer: String?,
    ) : AuthWithWebauthnSideEffect

    data object CloseAndNavigateToStartup : AuthWithWebauthnSideEffect
}
