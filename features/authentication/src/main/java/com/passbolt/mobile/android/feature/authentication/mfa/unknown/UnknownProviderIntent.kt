package com.passbolt.mobile.android.feature.authentication.mfa.unknown

sealed interface UnknownProviderIntent {
    data object Close : UnknownProviderIntent
}
