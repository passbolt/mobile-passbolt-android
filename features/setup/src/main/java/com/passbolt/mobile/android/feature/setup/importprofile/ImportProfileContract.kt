package com.passbolt.mobile.android.feature.setup.importprofile

import com.passbolt.mobile.android.core.mvp.BaseContract
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus

interface ImportProfileContract {

    interface View : BaseContract.View {
        fun showIncorrectUuid()
        fun clearValidationErrors()
        fun showIncorrectAccountUrl()
        fun showIncorrectPrivateKey()
        fun navigateToSummary(status: ResultStatus)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun userIdChanged(userId: String)
        fun accountUrlChanged(accountUrl: String)
        fun privateKeyChanged(privateKey: String)
        fun importClick()
    }
}
