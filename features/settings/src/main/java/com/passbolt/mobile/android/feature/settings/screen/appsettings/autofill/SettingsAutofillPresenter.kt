package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill

import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider

class SettingsAutofillPresenter(
    private val autofillInformationProvider: AutofillInformationProvider
) : SettingsAutofillContract.Presenter {

    override var view: SettingsAutofillContract.View? = null

    override fun attach(view: SettingsAutofillContract.View) {
        super.attach(view)
        refreshSwitchesState()
    }

    override fun viewResumed() {
        refreshSwitchesState()
    }

    private fun refreshSwitchesState() {
        handleAutofillServiceSwitchState()
        handleAccessibilityServiceSwitchState()
    }

    private fun handleAccessibilityServiceSwitchState() {
        if (autofillInformationProvider.isAccessibilityAutofillSetup()) {
            view?.setAccessibilitySwitchOn()
        } else {
            view?.setAccessibilitySwitchOff()
        }
    }

    private fun handleAutofillServiceSwitchState() {
        if (!autofillInformationProvider.isAutofillServiceSupported()) {
            view?.setAutofillSwitchOff()
        } else {
            if (autofillInformationProvider.isPassboltAutofillServiceSet()) {
                view?.setAutofillSwitchOn()
            } else {
                view?.setAutofillSwitchOff()
            }
        }
    }

    override fun autofillServiceSwitchClick() {
        if (autofillInformationProvider.isAutofillServiceSupported()) {
            if (autofillInformationProvider.isPassboltAutofillServiceSet()) {
                view?.showAutofillFeatureEnabledSuccess()
            } else {
                view?.showEncourageAutofillService()
            }
        } else {
            view?.showAutofillServiceNotSupported()
        }
    }

    override fun autofillSetupSuccessfully() {
        view?.showAutofillFeatureEnabledSuccess()
    }

    override fun accessibilityServiceSwitchClick() {
        view?.showEncourageAccessibilityService()
    }
}
