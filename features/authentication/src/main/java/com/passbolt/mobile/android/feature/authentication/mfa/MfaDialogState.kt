package com.passbolt.mobile.android.feature.authentication.mfa

sealed interface MfaDialogState {
    data class Totp(
        val authToken: String?,
        val hasOtherProviders: Boolean,
    ) : MfaDialogState

    data class Yubikey(
        val authToken: String?,
        val hasOtherProviders: Boolean,
    ) : MfaDialogState

    data class Duo(
        val authToken: String?,
        val hasOtherProviders: Boolean,
    ) : MfaDialogState

    data object UnknownProvider : MfaDialogState
}
