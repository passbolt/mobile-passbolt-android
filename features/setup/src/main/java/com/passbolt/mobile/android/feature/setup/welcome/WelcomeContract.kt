package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.mvp.BaseContract

interface WelcomeContract {

    interface View : BaseContract.View {
        fun showAccountCreationInfoDialog()
        fun navigateToTransferDetails()
        fun hideToolbar()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun noAccountButtonClick()
        fun connectToAccountClick()
        fun argsRetrieved(isTaskRoot: Boolean)
    }
}
