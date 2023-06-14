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

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

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
            val output = runAuthenticatedOperation(
                requireNotNull(presenter).needSessionRefreshFlow,
                requireNotNull(presenter).sessionRefreshedFlow
            ) {
                homeDataInteractor.refreshAllHomeScreenData()
            }
            _dataRefreshStatusFlow.emit(
                DataRefreshStatus.Finished(output)
            )
        }
    }
}
