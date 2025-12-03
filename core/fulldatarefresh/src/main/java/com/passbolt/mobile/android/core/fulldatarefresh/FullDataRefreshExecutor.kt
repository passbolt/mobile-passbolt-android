package com.passbolt.mobile.android.core.fulldatarefresh

import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Failure
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Success
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    coroutineLaunchContext: CoroutineLaunchContext,
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    fun performFullDataRefresh() {
        scope.launch {
            Timber.d("Full data refresh initiated")
            if (!dataRefreshTrackingFlow.isInProgress()) {
                dataRefreshTrackingFlow.updateStatus(InProgress)
                val output =
                    runAuthenticatedOperation {
                        homeDataInteractor.refreshAllHomeScreenData()
                    }

                dataRefreshTrackingFlow.updateStatus(
                    when (output) {
                        is Success -> FinishedWithSuccess
                        is Failure -> FinishedWithFailure
                    },
                )
            }
        }
    }

    suspend fun susPerformFullDataRefresh() {
        Timber.d("Full data refresh initiated")
        if (!dataRefreshTrackingFlow.isInProgress()) {
            dataRefreshTrackingFlow.updateStatus(InProgress)
            val output =
                runAuthenticatedOperation {
                    homeDataInteractor.refreshAllHomeScreenData()
                }

            dataRefreshTrackingFlow.updateStatus(
                when (output) {
                    is Success -> FinishedWithSuccess
                    is Failure -> FinishedWithFailure
                },
            )
        }
    }
}
