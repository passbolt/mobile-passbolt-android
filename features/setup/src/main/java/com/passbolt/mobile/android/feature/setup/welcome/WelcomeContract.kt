package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.mvp.BaseContract

interface WelcomeContract {

    interface View : BaseContract.View {
        fun showAccountCreationInfoDialog()
        fun navigateToTransferDetails()
        fun hideToolbar()
        fun showDeviceRootedDialog()
        fun showHelpMenu()
        fun initBackNavigation()
        fun navigateToImportProfile()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun noAccountButtonClick()
        fun connectToAccountClick()
        fun argsRetrieved(isTaskRoot: Boolean)
        fun helpClick()
        fun importProfileClick()
    }
}
