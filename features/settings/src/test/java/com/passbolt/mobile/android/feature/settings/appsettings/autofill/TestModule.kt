package com.passbolt.mobile.android.feature.settings.appsettings.autofill

import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.SettingsAutofillContract
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.SettingsAutofillPresenter
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockAutofillInformationProvider = mock<AutofillInformationProvider>()

val testSettingsAutofillModule = module {
    factory<SettingsAutofillContract.Presenter> {
        SettingsAutofillPresenter(
            autofillInformationProvider = mockAutofillInformationProvider
        )
    }
}
