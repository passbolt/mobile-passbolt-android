package com.passbolt.mobile.android.feature.authentication.mfa.totp

sealed interface EnterTotpIntent {
    data class ValidateOtp(
        val otp: String,
    ) : EnterTotpIntent

    data object PasteFromClipboard : EnterTotpIntent

    data object ChooseOtherProvider : EnterTotpIntent

    data object Close : EnterTotpIntent

    data class ToggleRememberMe(
        val checked: Boolean,
    ) : EnterTotpIntent
}
