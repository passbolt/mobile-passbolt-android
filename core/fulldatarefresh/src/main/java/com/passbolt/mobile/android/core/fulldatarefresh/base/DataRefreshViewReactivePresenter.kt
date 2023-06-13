package com.passbolt.mobile.android.core.fulldatarefresh.base

import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

abstract class DataRefreshViewReactivePresenter<V : DataRefreshViewReactiveContract.View>(
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<V>(coroutineLaunchContext),
    DataRefreshViewReactiveContract.Presenter<V>, KoinComponent {

    protected val fullDataRefreshExecutor: FullDataRefreshExecutor by inject()

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun resume(view: V) {
        fullDataRefreshExecutor.attach(this)

        coroutineScope.launch {
            fullDataRefreshExecutor.dataRefreshStatusFlow.collectLatest {
                when (it) {
                    is DataRefreshStatus.Finished -> {
                        this@DataRefreshViewReactivePresenter.view?.hideRefreshProgress()
                        when (it.output) {
                            is HomeDataInteractor.Output.Failure -> {
                                Timber.d("Full data refresh failed - executing failure action")
                                refreshFailureAction()
                            }
                            is HomeDataInteractor.Output.Success -> {
                                Timber.d("Full data refresh succeeded - executing success action")
                                refreshAction()
                            }
                        }
                    }
                    DataRefreshStatus.InProgress -> {
                        this@DataRefreshViewReactivePresenter.view?.showRefreshProgress()
                    }
                }
            }
        }
    }

    override fun pause() {
        fullDataRefreshExecutor.detach()
        coroutineScope.coroutineContext.cancelChildren()
    }
}
