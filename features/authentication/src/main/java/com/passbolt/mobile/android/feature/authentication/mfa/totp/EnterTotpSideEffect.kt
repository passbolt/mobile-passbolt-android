package com.passbolt.mobile.android.feature.authentication.mfa.totp

sealed interface EnterTotpSideEffect {
    data class NotifyVerificationSucceeded(
        val mfaHeader: String,
    ) : EnterTotpSideEffect

    data object NotifyLoginSucceeded : EnterTotpSideEffect

    data class NotifyChooseOtherProvider(
        val bearer: String?,
    ) : EnterTotpSideEffect

    data object CloseAndNavigateToStartup : EnterTotpSideEffect

    data class ShowErrorSnackbar(
        val kind: SnackbarErrorType,
    ) : EnterTotpSideEffect

    data object NavigateToLogin : EnterTotpSideEffect

    data object PasteOtp : EnterTotpSideEffect

    data object ClearOtp : EnterTotpSideEffect

    enum class SnackbarErrorType { GENERIC, NETWORK, WRONG_CODE, SESSION_EXPIRED }
}
