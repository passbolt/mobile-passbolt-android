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

package com.passbolt.mobile.android.tagsdetails

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.tagsdetails.ResourceTagsIntent.GoBack
import com.passbolt.mobile.android.tagsdetails.ResourceTagsIntent.Initialize
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.NavigateBack
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.NavigateToHome
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.ShowContentNotAvailable
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.tagsdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ResourceTagsViewModel(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
) : SideEffectViewModel<ResourceTagsState, ResourceTagsSideEffect>(ResourceTagsState()) {
    fun onIntent(intent: ResourceTagsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is Initialize -> {
                viewModelScope.launch(coroutineLaunchContext.io) {
                    synchronizeWithDataRefresh(intent.resourceId)
                }
                viewModelScope.launch(coroutineLaunchContext.io) {
                    loadData(intent.resourceId)
                }
            }
        }
    }

    private suspend fun synchronizeWithDataRefresh(resourceId: String) {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    updateViewState {
                        copy(isRefreshing = false)
                    }
                    loadData(resourceId)
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }

    private suspend fun loadData(resourceId: String) {
        try {
            val resourceResult =
                getLocalResourceUseCase.execute(
                    GetLocalResourceUseCase.Input(resourceId),
                )
            val tagsResult =
                getLocalResourceTagsUseCase.execute(
                    GetLocalResourceTagsUseCase.Input(resourceId),
                )
            updateViewState {
                copy(
                    resourceModel = resourceResult.resource,
                    tags = tagsResult.tags,
                )
            }
        } catch (_: NullPointerException) {
            emitSideEffect(ShowContentNotAvailable)
            emitSideEffect(NavigateToHome)
        } catch (throwable: Exception) {
            Timber.e(throwable)
        }
    }
}
