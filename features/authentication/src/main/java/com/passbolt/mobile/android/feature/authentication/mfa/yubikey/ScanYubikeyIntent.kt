package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

sealed interface ScanYubikeyIntent {
    data object ScanYubikey : ScanYubikeyIntent

    data object CancelYubikeyScan : ScanYubikeyIntent

    data class ValidateYubikeyOtp(
        val otp: String?,
    ) : ScanYubikeyIntent

    data object ChooseOtherProvider : ScanYubikeyIntent

    data object Close : ScanYubikeyIntent

    data class ToggleRememberMe(
        val checked: Boolean,
    ) : ScanYubikeyIntent

    data object DismissScanCancelledDialog : ScanYubikeyIntent

    data object DismissNotFromCurrentUserDialog : ScanYubikeyIntent
}
