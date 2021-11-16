package com.passbolt.mobile.android.feature.autofill.encourage

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.feature.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillContract
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillPresenter
import org.koin.dsl.module

internal val mockAutofillInformationProvider = mock<AutofillInformationProvider>()

val encourageAutofillModule = module {
    factory<EncourageAutofillContract.Presenter> {
        EncourageAutofillPresenter(mockAutofillInformationProvider)
    }
}
