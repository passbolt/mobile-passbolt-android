package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.mvp.BaseContract
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus

interface WelcomeContract {

    interface View : BaseContract.View {
        fun showAccountCreationInfoDialog()
        fun navigateToTransferDetails()
        fun hideToolbar()
        fun showDeviceRootedDialog()
        fun showHelpMenu()
        fun initBackNavigation()
        fun navigateToImportProfile()
        fun showAccountKitFilePicker()
        fun navigateToSummary(status: ResultStatus)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun noAccountButtonClick()
        fun connectToAccountClick()
        fun argsRetrieved(isTaskRoot: Boolean)
        fun helpClick()
        fun importProfileClick()
        fun accountKitSelected(accountKit: String)
        fun importAccountKitClick()
    }
}
