package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

sealed interface ScanYubikeySideEffect {
    data object LaunchYubikeyScan : ScanYubikeySideEffect

    data class NotifyVerificationSucceeded(
        val mfaHeader: String,
    ) : ScanYubikeySideEffect

    data object NotifyLoginSucceeded : ScanYubikeySideEffect

    data class NotifyChooseOtherProvider(
        val bearer: String?,
    ) : ScanYubikeySideEffect

    data object CloseAndNavigateToStartup : ScanYubikeySideEffect

    data class ShowErrorSnackbar(
        val kind: SnackbarErrorType,
    ) : ScanYubikeySideEffect

    data object NavigateToLogin : ScanYubikeySideEffect

    enum class SnackbarErrorType { GENERIC, SESSION_EXPIRED, EMPTY_OTP }
}
