package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility

sealed interface EncourageAccessibilitySideEffect {
    data object NavigateBack : EncourageAccessibilitySideEffect

    data object OpenOverlaySettings : EncourageAccessibilitySideEffect

    data object OpenAccessibilitySettings : EncourageAccessibilitySideEffect
}
