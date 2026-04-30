package com.passbolt.mobile.android.feature.authentication.mfa

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider

sealed interface MfaResult {
    data class Succeeded(
        val mfaHeader: String?,
    ) : MfaResult

    data class OtherProvider(
        val bearer: String?,
        val currentProvider: MfaProvider,
    ) : MfaResult
}
