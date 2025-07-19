package com.passbolt.mobile.android.core.fulldatarefresh.base

class DummyReactContract {
    interface View : DataRefreshViewReactiveContract.View {
        fun refreshActionUiEffect()

        fun refreshFailureActionUiEffect()
    }

    interface Presenter : DataRefreshViewReactiveContract.Presenter<View>
}
