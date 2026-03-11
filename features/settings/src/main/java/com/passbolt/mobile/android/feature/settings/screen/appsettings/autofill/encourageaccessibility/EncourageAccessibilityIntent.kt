package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility

sealed interface EncourageAccessibilityIntent {
    data object RefreshState : EncourageAccessibilityIntent

    data object EnableAccessibilityService : EncourageAccessibilityIntent

    data object GrantOverlayPermission : EncourageAccessibilityIntent

    data object Close : EncourageAccessibilityIntent

    data object ConsentToEnableAccessibility : EncourageAccessibilityIntent

    data object DismissEnableAccessibilityConsent : EncourageAccessibilityIntent
}
