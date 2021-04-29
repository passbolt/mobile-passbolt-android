package com.passbolt.mobile.android.feature.setup.welcome

import javax.inject.Inject

class WelcomePresenter @Inject constructor() : WelcomeContract.Presenter {

    override var view: WelcomeContract.View? = null

    override fun noAccountButtonClick() {
        view?.showAccountCreationInfoDialog()
    }

    override fun connectToAccountClick() {
        view?.navigateToTransferDetails()
    }
}
