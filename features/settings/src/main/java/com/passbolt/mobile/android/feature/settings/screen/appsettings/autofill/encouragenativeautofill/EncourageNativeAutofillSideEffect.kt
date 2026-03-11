package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill

sealed interface EncourageNativeAutofillSideEffect {
    data object NavigateBack : EncourageNativeAutofillSideEffect

    data object AutofillEnabled : EncourageNativeAutofillSideEffect

    data object OpenAutofillSettings : EncourageNativeAutofillSideEffect
}
