package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility

data class EncourageAccessibilityState(
    val isAccessibilityServiceEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val showAccessibilityConsent: Boolean = false,
)
