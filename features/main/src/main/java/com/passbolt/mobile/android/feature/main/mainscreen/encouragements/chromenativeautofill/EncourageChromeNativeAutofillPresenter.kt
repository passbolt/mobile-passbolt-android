package com.passbolt.mobile.android.feature.main.mainscreen.encouragements.chromenativeautofill

import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.EncouragementsInteractor

class EncourageChromeNativeAutofillPresenter(
    private val encouragementsInteractor: EncouragementsInteractor,
    private val autofillInformationProvider: AutofillInformationProvider
) : EncourageChromeNativeAutofillContract.Presenter {

    override var view: EncourageChromeNativeAutofillContract.View? = null

    override fun attach(view: EncourageChromeNativeAutofillContract.View) {
        super.attach(view)
        encouragementsInteractor.chromeNativeAutofillEncouragementShown()
    }

    override fun resume() {
        if (autofillInformationProvider.getChromeNativeAutofillStatus() == ENABLED) {
            view?.notifyChromeNativeAutofillSetUp()
        }
    }

    override fun goToChromeNativeAutofillSettingsClick() {
        view?.launchChromeNativeAutofillDeeplink()
    }

    override fun closeClick() {
        view?.close()
    }

    override fun maybeLaterClick() {
        view?.close()
    }
}
