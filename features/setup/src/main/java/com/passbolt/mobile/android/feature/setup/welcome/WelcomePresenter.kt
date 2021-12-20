package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.security.rootdetection.RootDetector

class WelcomePresenter(
    private val rootDetector: RootDetector
) : WelcomeContract.Presenter {

    override var view: WelcomeContract.View? = null

    override fun argsRetrieved(isTaskRoot: Boolean) {
        if (!isTaskRoot) {
            view?.initBackNavigation()
        }
        if (rootDetector.isDeviceRooted()) {
            view?.showDeviceRootedDialog()
        }
    }

    override fun noAccountButtonClick() {
        view?.showAccountCreationInfoDialog()
    }

    override fun connectToAccountClick() {
        view?.navigateToTransferDetails()
    }

    override fun helpClick() {
        view?.showHelpMenu()
    }
}
