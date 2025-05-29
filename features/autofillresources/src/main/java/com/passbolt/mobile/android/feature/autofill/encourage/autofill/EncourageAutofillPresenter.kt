package com.passbolt.mobile.android.feature.autofill.encourage.autofill

import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider

class EncourageAutofillPresenter(
    private val autofillInformationProvider: AutofillInformationProvider,
) : EncourageAutofillContract.Presenter {
    override var view: EncourageAutofillContract.View? = null

    override fun goToSettingsClick() {
        if (!autofillInformationProvider.isAutofillServiceSupported()) {
            view?.showAutofillNotSupported()
        } else {
            view?.openAutofillSettings()
        }
    }

    override fun closeClick() {
        view?.close()
    }

    override fun maybeLaterClick() {
        view?.close()
    }

    override fun autofillSettingsClosedWithResult() {
        if (autofillInformationProvider.isPassboltAutofillServiceSet()) {
            view?.closeWithSuccess()
        }
    }
}
