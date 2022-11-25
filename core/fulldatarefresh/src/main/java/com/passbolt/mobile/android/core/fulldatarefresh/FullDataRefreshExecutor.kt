package com.passbolt.mobile.android.core.fulldatarefresh

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class FullDataRefreshExecutor(
    private val homeDataInteractor: HomeDataInteractor,
    coroutineLaunchContext: CoroutineLaunchContext
) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    val dataRefreshStatusFlow: Flow<DataRefreshStatus>
        get() = _dataRefreshStatusFlow
    private val _dataRefreshStatusFlow = MutableSharedFlow<DataRefreshStatus>(replay = 1)
    private var presenter: BaseAuthenticatedPresenter<BaseAuthenticatedContract.View>? = null

    fun <V : BaseAuthenticatedContract.View, P : BaseAuthenticatedPresenter<V>> attach(presenter: P) {
        Timber.d("Refresh executor attaching to: ${presenter.javaClass.name}")
        this.presenter = presenter as BaseAuthenticatedPresenter<BaseAuthenticatedContract.View>
    }

    fun detach() {
        Timber.d("Refresh executor detaching from: ${presenter?.javaClass?.name}")
        presenter = null
    }

    fun performFullDataRefresh() {
        scope.launch {
            Timber.d("Full data refresh initiated")
            _dataRefreshStatusFlow.emit(DataRefreshStatus.InProgress)
            val output =
                runAuthenticatedOperation(
                    requireNotNull(presenter).needSessionRefreshFlow,
                    requireNotNull(presenter).sessionRefreshedFlow
                ) {
                    homeDataInteractor.refreshAllHomeScreenData()
                }
            _dataRefreshStatusFlow.emit(
                DataRefreshStatus.Finished(
                    output
                )
            )
        }
    }
}
