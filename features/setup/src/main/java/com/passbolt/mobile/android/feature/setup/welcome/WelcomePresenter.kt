package com.passbolt.mobile.android.feature.setup.welcome

class WelcomePresenter : WelcomeContract.Presenter {

    override var view: WelcomeContract.View? = null

    override fun argsRetrieved(isTaskRoot: Boolean) {
        if (isTaskRoot) {
            view?.hideToolbar()
        }
    }

    override fun noAccountButtonClick() {
        view?.showAccountCreationInfoDialog()
    }

    override fun connectToAccountClick() {
        view?.navigateToTransferDetails()
    }
}
