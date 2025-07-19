package com.passbolt.mobile.android.core.fulldatarefresh.base

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext

class DummyReactPresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
) : DataRefreshViewReactivePresenter<DummyReactContract.View>(coroutineLaunchContext),
    DummyReactContract.Presenter {
    override var view: DummyReactContract.View? = null

    override fun refreshSuccessAction() {
        view?.refreshActionUiEffect()
    }

    override fun refreshFailureAction() {
        view?.refreshFailureActionUiEffect()
    }
}
