package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase

class WelcomePresenter(
    private val rootDetector: RootDetector,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase
) : WelcomeContract.Presenter {

    override var view: WelcomeContract.View? = null

    override fun argsRetrieved(isTaskRoot: Boolean) {
        if (!isTaskRoot) {
            view?.initBackNavigation()
        }
        if (!getGlobalPreferencesUseCase.execute(Unit).isHideRootDialogEnabled && rootDetector.isDeviceRooted()) {
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

    override fun importProfileClick() {
        view?.navigateToImportProfile()
    }
}
