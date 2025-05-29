package com.passbolt.mobile.android.feature.autofill.encourage

import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillContract
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillPresenter
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockAutofillInformationProvider = mock<AutofillInformationProvider>()

val encourageAutofillModule =
    module {
        factory<EncourageAutofillContract.Presenter> {
            EncourageAutofillPresenter(mockAutofillInformationProvider)
        }
    }
