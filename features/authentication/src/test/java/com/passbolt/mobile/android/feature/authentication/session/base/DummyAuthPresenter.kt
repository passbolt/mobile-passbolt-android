package com.passbolt.mobile.android.feature.authentication.session.base

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation

class DummyAuthPresenter(coroutineLaunchContext: CoroutineLaunchContext) :
    DummyAuthContract.Presenter, BaseAuthenticatedPresenter<DummyAuthContract.View>(coroutineLaunchContext) {
    override var view: DummyAuthContract.View? = null

    override suspend fun authenticatedOperation() {
        runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            object : AuthenticatedUseCaseOutput {
                override val authenticationState = AuthenticationState.Authenticated
            }
        }
    }
}
