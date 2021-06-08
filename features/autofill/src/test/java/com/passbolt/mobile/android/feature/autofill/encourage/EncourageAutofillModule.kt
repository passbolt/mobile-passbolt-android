package com.passbolt.mobile.android.feature.autofill.encourage

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.feature.autofill.info.AutofillInformationProvider
import org.koin.dsl.module

internal val mockAutofillInformationProvider = mock<AutofillInformationProvider>()

val encourageAutofillModule = module {
    factory<EncourageAutofillContract.Presenter> {
        EncourageAutofillPresenter(mockAutofillInformationProvider)
    }
}