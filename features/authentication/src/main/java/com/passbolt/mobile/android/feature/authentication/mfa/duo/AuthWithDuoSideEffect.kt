package com.passbolt.mobile.android.feature.authentication.mfa.duo

sealed interface AuthWithDuoSideEffect {
    data class NotifyVerificationSucceeded(
        val mfaHeader: String,
    ) : AuthWithDuoSideEffect

    data object NotifyLoginSucceeded : AuthWithDuoSideEffect

    data class NotifyOtherProviderClicked(
        val bearer: String?,
    ) : AuthWithDuoSideEffect

    data object CloseAndNavigateToStartup : AuthWithDuoSideEffect

    data class ShowErrorSnackbar(
        val kind: SnackbarErrorType,
    ) : AuthWithDuoSideEffect

    data object NavigateToLogin : AuthWithDuoSideEffect

    enum class SnackbarErrorType { GENERIC, SESSION_EXPIRED }
}
