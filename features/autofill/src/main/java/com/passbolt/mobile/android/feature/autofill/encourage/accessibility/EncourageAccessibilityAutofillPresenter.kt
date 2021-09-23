package com.passbolt.mobile.android.feature.autofill.encourage.accessibility

import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.feature.autofill.accessibility.AccessibilityService

class EncourageAccessibilityAutofillPresenter(
    private val autofillInformationProvider: AutofillInformationProvider
) : EncourageAccessibilityAutofillContract.Presenter {

    override var view: EncourageAccessibilityAutofillContract.View? = null

    override fun resume() {
        view?.setOverlayEnabled(autofillInformationProvider.isOverlayEnabled())
        view?.setAccessibilityServiceEnabled(
            autofillInformationProvider.isAccessibilityServiceEnabled(
                AccessibilityService::class.java.name
            )
        )
    }

    private fun isSetupSuccessful() =
        autofillInformationProvider.isOverlayEnabled() && autofillInformationProvider.isAccessibilityServiceEnabled(
            AccessibilityService::class.java.name
        )

    override fun closeClick() {
        if (isSetupSuccessful()) {
            view?.closeWithSuccess()
        } else {
            view?.dismissWithNoAction()
        }
    }

    override fun maybeLaterClick() {
        view?.dismissWithNoAction()
    }

    override fun overlayClick() {
        view?.navigateToOverlayTutorial()
    }

    override fun serviceClick() {
        view?.navigateToServiceTutorial()
    }

    override fun backPressed() {
        if (isSetupSuccessful()) {
            view?.closeWithSuccess()
        } else {
            view?.dismissWithNoAction()
        }
    }
}
