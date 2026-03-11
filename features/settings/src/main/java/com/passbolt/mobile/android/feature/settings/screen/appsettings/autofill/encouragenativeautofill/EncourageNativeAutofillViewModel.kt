package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill

import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.DismissAutofillNotSupported
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.EnableAutofillService
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.SettingsResult
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Skip
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.AutofillEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.OpenAutofillSettings

internal class EncourageNativeAutofillViewModel(
    private val autofillInformationProvider: AutofillInformationProvider,
) : SideEffectViewModel<EncourageNativeAutofillState, EncourageNativeAutofillSideEffect>(
        EncourageNativeAutofillState(),
    ) {
    fun onIntent(intent: EncourageNativeAutofillIntent) {
        when (intent) {
            EnableAutofillService -> {
                if (!autofillInformationProvider.isAutofillServiceSupported()) {
                    updateViewState { copy(showAutofillNotSupported = true) }
                } else {
                    emitSideEffect(OpenAutofillSettings)
                }
            }
            SettingsResult -> {
                if (autofillInformationProvider.isPassboltAutofillServiceSet()) {
                    emitSideEffect(AutofillEnabled)
                }
            }
            Skip -> emitSideEffect(NavigateBack)
            Close -> emitSideEffect(NavigateBack)
            DismissAutofillNotSupported -> updateViewState { copy(showAutofillNotSupported = false) }
        }
    }
}
