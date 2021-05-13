package com.passbolt.mobile.android.feature.setup.welcome

class WelcomePresenter : WelcomeContract.Presenter {

    override var view: WelcomeContract.View? = null

    override fun noAccountButtonClick() {
        view?.showAccountCreationInfoDialog()
    }

    override fun connectToAccountClick() {
        view?.navigateToTransferDetails()
    }
}
