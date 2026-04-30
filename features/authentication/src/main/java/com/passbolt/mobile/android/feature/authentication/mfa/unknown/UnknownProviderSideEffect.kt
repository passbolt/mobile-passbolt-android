package com.passbolt.mobile.android.feature.authentication.mfa.unknown

sealed interface UnknownProviderSideEffect {
    data object CloseAndNavigateToStartup : UnknownProviderSideEffect
}
