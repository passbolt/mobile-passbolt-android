package com.passbolt.mobile.android.ui

import java.time.LocalDateTime

data class GlobalPreferencesUiModel(
    val areDebugLogsEnabled: Boolean,
    val debugLogFileCreationDateTime: LocalDateTime?,
    val debugLogLastAppVersion: String?,
    val isHideRootDialogEnabled: Boolean,
    val isAuthRequiredOnEveryEntry: Boolean,
    val apiFetchPageSize: Int,
    val isApiFetchPageSizeManuallySet: Boolean,
    val accessibilityPoliciesConsentGiven: Boolean,
    val isCopyTotpOnAutofillEnabled: Boolean,
)
