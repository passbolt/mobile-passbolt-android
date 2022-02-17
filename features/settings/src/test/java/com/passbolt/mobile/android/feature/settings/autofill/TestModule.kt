package com.passbolt.mobile.android.feature.settings.autofill

import org.mockito.kotlin.mock
import com.passbolt.mobile.android.feature.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.feature.settings.screen.autofill.SettingsAutofillContract
import com.passbolt.mobile.android.feature.settings.screen.autofill.SettingsAutofillPresenter
import org.koin.dsl.module

internal val mockAutofillInformationProvider = mock<AutofillInformationProvider>()

val testSettingsAutofillModule = module {
    factory<SettingsAutofillContract.Presenter> {
        SettingsAutofillPresenter(
            autofillInformationProvider = mockAutofillInformationProvider
        )
    }
}
