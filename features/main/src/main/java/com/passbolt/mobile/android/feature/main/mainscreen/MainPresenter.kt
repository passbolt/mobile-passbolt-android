package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.DataRefreshStatus
import com.passbolt.mobile.android.feature.home.screen.interactor.HomeDataInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import timber.log.Timber

class MainPresenter(
    private val homeDataInteractor: HomeDataInteractor,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<MainContract.View>(coroutineLaunchContext), MainContract.Presenter {

    override var view: MainContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private val _dataRefreshStatusFlow = MutableSharedFlow<DataRefreshStatus>(replay = 1)
    override val dataRefreshFinishedStatusFlow: Flow<DataRefreshStatus.Finished> = _dataRefreshStatusFlow
        .filterIsInstance()

    override fun attach(view: MainContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun performFullDataRefresh() {
        scope.launch {
            Timber.d("Full data refresh initiated")
            val output = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                homeDataInteractor.refreshAllHomeScreenData()
            }
            _dataRefreshStatusFlow.emit(DataRefreshStatus.Finished(output))
        }
    }
}
