package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility

import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.ConsentToEnableAccessibility
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.DismissEnableAccessibilityConsent
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.EnableAccessibilityService
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.GrantOverlayPermission
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.RefreshState
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenAccessibilitySettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenOverlaySettings

internal class EncourageAccessibilityViewModel(
    private val autofillInformationProvider: AutofillInformationProvider,
) : SideEffectViewModel<EncourageAccessibilityState, EncourageAccessibilitySideEffect>(
        EncourageAccessibilityState(),
    ) {
    init {
        refreshState()
    }

    fun onIntent(intent: EncourageAccessibilityIntent) {
        when (intent) {
            RefreshState -> refreshState()
            EnableAccessibilityService -> enableAccessibilityService()
            GrantOverlayPermission -> emitSideEffect(OpenOverlaySettings)
            Close -> emitSideEffect(NavigateBack)
            ConsentToEnableAccessibility -> {
                updateViewState { copy(showAccessibilityConsent = false) }
                emitSideEffect(OpenAccessibilitySettings)
            }
            DismissEnableAccessibilityConsent -> updateViewState { copy(showAccessibilityConsent = false) }
        }
    }

    private fun refreshState() {
        updateViewState {
            copy(
                isAccessibilityServiceEnabled = autofillInformationProvider.isAccessibilityServiceEnabled(),
                isOverlayPermissionGranted = autofillInformationProvider.isAccessibilityOverlayEnabled(),
            )
        }
    }

    private fun enableAccessibilityService() {
        if (!autofillInformationProvider.isAccessibilityServiceEnabled()) {
            updateViewState { copy(showAccessibilityConsent = true) }
        } else {
            emitSideEffect(OpenAccessibilitySettings)
        }
    }
}
