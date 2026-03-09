package com.passbolt.mobile.android.feature.authentication.mfa.totp

data class EnterTotpState(
    val showProgress: Boolean = false,
    val hasOtherProvider: Boolean = false,
    val rememberMe: Boolean = true,
    val otpTextColor: OtpTextColor = OtpTextColor.DEFAULT,
) {
    enum class OtpTextColor { DEFAULT, ERROR }
}
