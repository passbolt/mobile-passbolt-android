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

    override fun overlayClick() {
        // TODO temporary disable tutorial - broken links
        // view?.navigateToOverlayTutorial()
        view?.navigateToOverlaySettings()
    }

    override fun serviceClick() {
        // TODO temporary disable tutorial - broken links
        // view?.navigateToServiceTutorial()
        view?.navigateToServiceSettings()
    }

    override fun backPressed() {
        view?.dismissWithNotify()
    }

    override fun possibleAutofillChange() {
        if (autofillInformationProvider.isAccessibilityAutofillSetup()) {
            view?.dismissWithNoAction()
            view?.showAutofillEnabledDialog()
        }
        view?.notifyPossibleAutofillChange()
    }
}
