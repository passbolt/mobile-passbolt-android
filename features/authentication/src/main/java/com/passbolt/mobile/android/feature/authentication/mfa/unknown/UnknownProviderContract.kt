package com.passbolt.mobile.android.feature.authentication.mfa.unknown

import com.passbolt.mobile.android.core.mvp.BaseContract

interface UnknownProviderContract {

    interface View : BaseContract.View {
        fun showProgress()
        fun hideProgress()
        fun closeAndNavigateToStartup()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun closeClick()
    }
}
