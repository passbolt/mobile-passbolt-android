package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.mvp.BaseContract

interface WelcomeContract {

    interface View : BaseContract.View {
        fun showAccountCreationInfoDialog()
        fun navigateToTransferDetails()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun noAccountButtonClick()
        fun connectToAccountClick()
    }
}