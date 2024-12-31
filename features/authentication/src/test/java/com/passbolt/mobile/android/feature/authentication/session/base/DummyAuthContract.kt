package com.passbolt.mobile.android.feature.authentication.session.base

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract

class DummyAuthContract {
    interface View : BaseAuthenticatedContract.View

    interface Presenter : BaseAuthenticatedContract.Presenter<View> {
        suspend fun authenticatedOperation()
    }
}
