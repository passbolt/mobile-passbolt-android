package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill

sealed interface EncourageNativeAutofillIntent {
    data object EnableAutofillService : EncourageNativeAutofillIntent

    data object SettingsResult : EncourageNativeAutofillIntent

    data object Skip : EncourageNativeAutofillIntent

    data object Close : EncourageNativeAutofillIntent

    data object DismissAutofillNotSupported : EncourageNativeAutofillIntent
}
