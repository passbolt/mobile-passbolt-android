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

package com.passbolt.mobile.android.folderdetails

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoBack
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoToLocationDetails
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoToPermissionDetails
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.Initialize
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.SharedWithClick
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToFolderLocation
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToFolderPermissions
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToHome
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateUp
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.ShowToast
import com.passbolt.mobile.android.folderdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.folderdetails.ToastType.CONTENT_NOT_AVAILABLE
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.launch
import timber.log.Timber

internal class FolderDetailsViewModel(
    private val getLocalFolderDetailsUseCase: GetLocalFolderDetailsUseCase,
    private val getLocalFolderLocationUseCase: GetLocalFolderLocationUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
) : AuthenticatedViewModel<FolderDetailsState, FolderDetailsSideEffect>(FolderDetailsState()) {
    val folderId: String
        get() = requireNotNull(viewState.value.folderId)

    fun onIntent(intent: FolderDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            is Initialize -> initialize(intent.folderId)
            GoToLocationDetails -> emitSideEffect(NavigateToFolderLocation(folderId))
            GoToPermissionDetails -> emitSideEffect(NavigateToFolderPermissions(folderId, PermissionsMode.VIEW))
            SharedWithClick -> emitSideEffect(NavigateToFolderPermissions(folderId, PermissionsMode.VIEW))
        }
    }

    private fun initialize(folderId: String) {
        updateViewState { copy(folderId = folderId) }

        viewModelScope.launch(coroutineLaunchContext.io) {
            loadFolderDetails(folderId)
        }

        viewModelScope.launch(coroutineLaunchContext.io) {
            synchronizeWithDataRefresh(folderId)
        }
    }

    private suspend fun loadFolderDetails(folderId: String) {
        try {
            val folder =
                getLocalFolderDetailsUseCase
                    .execute(GetLocalFolderDetailsUseCase.Input(folderId))
                    .folder

            val parentFolders =
                getLocalFolderLocationUseCase
                    .execute(GetLocalFolderLocationUseCase.Input(folderId))
                    .parentFolders

            val canViewPermissions =
                getRbacRulesUseCase
                    .execute(Unit)
                    .rbacModel
                    .shareViewRule == ALLOW

            val permissions =
                if (canViewPermissions) {
                    getLocalFolderPermissionsUseCase
                        .execute(GetLocalFolderPermissionsUseCase.Input(folderId))
                        .permissions
                } else {
                    emptyList()
                }

            updateViewState {
                copy(
                    folder = folder,
                    locationPath = parentFolders.map { it.name },
                    canViewPermissions = canViewPermissions,
                    permissions = permissions,
                )
            }
        } catch (_: NullPointerException) {
            emitSideEffect(ShowToast(CONTENT_NOT_AVAILABLE))
            emitSideEffect(NavigateToHome)
        } catch (throwable: Exception) {
            Timber.e(throwable)
        }
    }

    private suspend fun synchronizeWithDataRefresh(folderId: String) {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    updateViewState { copy(isRefreshing = false) }
                    loadFolderDetails(folderId)
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }
}
