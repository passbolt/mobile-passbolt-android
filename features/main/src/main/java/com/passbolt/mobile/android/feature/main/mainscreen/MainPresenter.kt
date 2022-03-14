package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.home.screen.DataRefreshStatus
import com.passbolt.mobile.android.feature.home.screen.interactor.HomeDataInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class MainPresenter(
    private val homeDataInteractor: HomeDataInteractor,
    coroutineLaunchContext: CoroutineLaunchContext
) : MainContract.Presenter {

    override var view: MainContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private val _dataRefreshStatusFlow = MutableStateFlow<DataRefreshStatus>(DataRefreshStatus.NotInitialized)
    override val dataRefreshFinishedStatusFlow = _dataRefreshStatusFlow
        .filterIsInstance<DataRefreshStatus.Finished>()

    override fun performFullDataRefresh(): Flow<DataRefreshStatus.Finished> {
        scope.launch {
            val output = homeDataInteractor.refreshAllHomeScreenData()
            _dataRefreshStatusFlow.value = DataRefreshStatus.Finished(output)
        }
        return dataRefreshFinishedStatusFlow
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
