package com.passbolt.mobile.android.feature.authentication.compose

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason
import com.passbolt.mobile.android.core.navigation.ActivityIntents

sealed interface AuthenticationSideEffect {
    data class ShowAuth(
        val type: ActivityIntents.AuthConfig,
    ) : AuthenticationSideEffect

    data class ShowMfaAuth(
        val mfaReason: Reason.Mfa.MfaProvider?,
        val hasMultipleProviders: Boolean,
        val sessionAccessToken: String?,
    ) : AuthenticationSideEffect
}
