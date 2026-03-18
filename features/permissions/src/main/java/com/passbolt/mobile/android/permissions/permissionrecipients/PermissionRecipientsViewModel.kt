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

package com.passbolt.mobile.android.permissions.permissionrecipients

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetLocalGroupsUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUsersUseCase
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper.Companion.TEMPORARY_NEW_PERMISSION_ID
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.GoBack
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.Save
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.Search
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleGroupSelection
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleUserSelection
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBackWithResult
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import kotlinx.coroutines.launch

class PermissionRecipientsViewModel(
    alreadyAddedGroupPermissions: Array<PermissionModelUi.GroupPermissionModel>,
    alreadyAddedUserPermissions: Array<PermissionModelUi.UserPermissionModel>,
    private val getLocalGroupsUseCase: GetLocalGroupsUseCase,
    private val getLocalUsersUseCase: GetLocalUsersUseCase,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val searchableMatcher: SearchableMatcher,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : AuthenticatedViewModel<PermissionRecipientsState, PermissionRecipientsSideEffect>(
        initialState =
            PermissionRecipientsState(
                currentPermissions = alreadyAddedGroupPermissions.toList() + alreadyAddedUserPermissions.toList(),
            ),
    ) {
    private val alreadyAddedGroups = alreadyAddedGroupPermissions.toList()
    private val alreadyAddedUsers = alreadyAddedUserPermissions.toList()

    init {
        loadRecipients()
    }

    fun onIntent(intent: PermissionRecipientsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is Search -> search(intent.query)
            is ToggleGroupSelection -> toggleGroupSelection(intent.group)
            is ToggleUserSelection -> toggleUserSelection(intent.user)
            Save -> save()
        }
    }

    private fun loadRecipients() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val groups =
                getLocalGroupsUseCase
                    .execute(
                        GetLocalGroupsUseCase.Input(alreadyAddedGroups.map { it.group.groupId }),
                    ).groups

            val users =
                getLocalUsersUseCase
                    .execute(
                        GetLocalUsersUseCase.Input(alreadyAddedUsers.map { it.user.userId }),
                    ).users

            updateViewState {
                copy(
                    groups = groups,
                    users = users,
                    filteredGroups = groups,
                    filteredUsers = users,
                )
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val state = viewState.value
            val filteredGroups = state.groups.filter { searchableMatcher.matches(it, query) }
            val filteredUsers = state.users.filter { searchableMatcher.matches(it, query) }

            val filteredExistingPermissions =
                if (query.isNotBlank()) {
                    (alreadyAddedGroups + alreadyAddedUsers)
                        .filter { searchableMatcher.matches(it, query) }
                } else {
                    emptyList()
                }

            val shouldShowEmptyState =
                query.isNotBlank() &&
                    filteredGroups.isEmpty() &&
                    filteredUsers.isEmpty() &&
                    filteredExistingPermissions.isEmpty()

            updateViewState {
                copy(
                    filteredGroups = filteredGroups,
                    filteredUsers = filteredUsers,
                    searchInputEndIconMode =
                        if (query.isNotBlank()) {
                            SearchInputEndIconMode.CLEAR
                        } else {
                            SearchInputEndIconMode.NONE
                        },
                    filteredExistingPermissions = filteredExistingPermissions,
                    showEmptyState = shouldShowEmptyState,
                )
            }
        }
    }

    private fun toggleGroupSelection(group: GroupModel) {
        val currentIds = viewState.value.selectedGroupIds
        val newIds =
            if (group.groupId in currentIds) {
                currentIds - group.groupId
            } else {
                currentIds + group.groupId
            }
        updateViewState { copy(selectedGroupIds = newIds) }
        recomputeDisplayPermissions()
    }

    private fun toggleUserSelection(user: UserModel) {
        val currentIds = viewState.value.selectedUserIds
        val newIds =
            if (user.id in currentIds) {
                currentIds - user.id
            } else {
                currentIds + user.id
            }
        updateViewState { copy(selectedUserIds = newIds) }
        recomputeDisplayPermissions()
    }

    private fun recomputeDisplayPermissions() {
        val state = viewState.value
        val selectedPermissions = buildSelectedPermissions(state)
        updateViewState {
            copy(currentPermissions = alreadyAddedGroups + alreadyAddedUsers + selectedPermissions)
        }
    }

    private fun buildSelectedPermissions(state: PermissionRecipientsState): List<PermissionModelUi> {
        val selectedGroups =
            state.groups
                .filter { it.groupId in state.selectedGroupIds }
                .map {
                    permissionsModelMapper.map(
                        it,
                        DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS,
                        TEMPORARY_NEW_PERMISSION_ID,
                    )
                }
        val selectedUsers =
            state.users
                .filter { it.id in state.selectedUserIds }
                .map {
                    permissionsModelMapper.map(
                        it,
                        DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS,
                        TEMPORARY_NEW_PERMISSION_ID,
                    )
                }
        return selectedGroups + selectedUsers
    }

    private fun save() {
        val selectedPermissions = buildSelectedPermissions(viewState.value)
        val allPermissions = alreadyAddedGroups + alreadyAddedUsers + selectedPermissions
        emitSideEffect(NavigateBackWithResult(allPermissions))
    }

    companion object {
        val DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS = ResourcePermission.READ
    }
}
