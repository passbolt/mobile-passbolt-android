package com.passbolt.mobile.android.feature.autofill.encourage.accessibility

import com.passbolt.mobile.android.feature.autofill.AutofillInformationProvider

class EncourageAccessibilityAutofillPresenter(
    private val autofillInformationProvider: AutofillInformationProvider
) : EncourageAccessibilityAutofillContract.Presenter {

    override var view: EncourageAccessibilityAutofillContract.View? = null

    override fun resume() {
        view?.setOverlayEnabled(autofillInformationProvider.isAccessibilityOverlayEnabled())
        view?.setAccessibilityServiceEnabled(
            autofillInformationProvider.isAccessibilityServiceEnabled()
        )
    }

    override fun closeClick() {
        view?.dismissWithNotify()
    }

    override fun maybeLaterClick() {
        view?.dismissWithNotify()
    }

    override fun overlayClick() {
        view?.navigateToOverlayTutorial()
    }

    override fun serviceClick() {
        view?.navigateToServiceTutorial()
    }

    override fun backPressed() {
        view?.dismissWithNotify()
    }

    override fun possibleAutofillChange() {
        if (autofillInformationProvider.isAccessibilityAutofillSetup()) {
            view?.dismissWithNoAction()
        }
        view?.notifyPossibleAutofillChange()
    }
}
