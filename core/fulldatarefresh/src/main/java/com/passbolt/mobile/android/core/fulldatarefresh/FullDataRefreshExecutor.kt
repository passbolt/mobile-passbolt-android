package com.passbolt.mobile.android.core.fulldatarefresh

import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Failure
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Success
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.ServerReachabilityTracker
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.domain.secrets.usecase.offline.OfflineSignInGate
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

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
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val offlineSessionState: OfflineSessionState,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val serverReachabilityTracker: ServerReachabilityTracker,
    private val offlineSignInGate: OfflineSignInGate,
) {
    suspend fun performFullDataRefresh() {
        Timber.d("Full data refresh initiated")
        if (offlineSessionState.isOfflineSession && !tryLeaveOfflineSession()) {
            Timber.d("Full data refresh skipped - offline session and the server is still unreachable")
            dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)
            return
        }
        if (!dataRefreshTrackingFlow.isInProgress()) {
            val startedAt = serverReachabilityTracker.now()
            dataRefreshTrackingFlow.updateStatus(InProgress(progress = 0f))
            val output =
                runAuthenticatedOperation {
                    withContext(coroutineLaunchContext.default) {
                        homeDataInteractor.refreshAllHomeScreenData { progress ->
                            dataRefreshTrackingFlow.updateStatus(InProgress(progress))
                        }
                    }
                }

            when (output) {
                is Success -> {
                    dataRefreshTrackingFlow.updateStatus(InProgress(progress = 1f))
                    delay(FULL_PROGRESS_DISPLAY_MILLIS.milliseconds)
                    dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)
                }
                is Failure -> {
                    if (output.authenticationState is AuthenticationState.Authenticated &&
                        serverReachabilityTracker.unreachableSince(startedAt)
                    ) {
                        getSelectedAccountUseCase.execute(Unit).selectedAccount?.let { tryEnterOfflineSession(it) }
                    }
                    dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)
                }
            }
        }
    }

    /**
     * The refresh failed because the server could not be reached (not because it answered
     * with an error). If the user opted in to offline mode and the cache is still within
     * its retention window, switch to an offline session right away: the offline banner
     * appears, secrets come from the cache and the UI turns read-only - instead of a
     * "failed to refresh" error and an offline session only at the next sign-in.
     */
    private suspend fun tryEnterOfflineSession(accountId: String) {
        when (val gate = offlineSignInGate.evaluate(accountId)) {
            is OfflineSignInGate.Result.Allowed -> {
                Timber.d("Server unreachable during refresh - entering offline session")
                offlineSessionState.enterOfflineSession()
            }
            else -> Timber.d("Server unreachable during refresh - offline session not possible: $gate")
        }
    }

    /**
     * During an offline session a refresh is only possible if the server is back: a
     * session refresh with the stored refresh token is the cheapest probe, and on success
     * it also restores the JWT, so the refresh below runs as a normal online one.
     */
    private suspend fun tryLeaveOfflineSession(): Boolean =
        when (refreshSessionUseCase.execute(Unit)) {
            is RefreshSessionUseCase.Output.Success -> {
                Timber.d("Server reachable again - leaving offline session")
                offlineSessionState.exitOfflineSession()
                true
            }
            is RefreshSessionUseCase.Output.Failure -> false
        }

    private companion object {
        private const val FULL_PROGRESS_DISPLAY_MILLIS = 300L
    }
}
