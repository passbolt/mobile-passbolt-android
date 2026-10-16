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
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.NONE
import com.passbolt.mobile.android.domain.groups.usecase.GetLocalGroupsUseCase
import com.passbolt.mobile.android.domain.users.usecase.GetLocalUsersUseCase
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
import com.passbolt.mobile.android.ui.UserUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class PermissionRecipientsViewModel(
    alreadyAddedGroupPermissions: Array<PermissionModelUi.GroupPermissionModel>,
    alreadyAddedUserPermissions: Array<PermissionModelUi.UserPermissionModel>,
    private val getLocalGroupsUseCase: GetLocalGroupsUseCase,
    private val getLocalUsersUseCase: GetLocalUsersUseCase,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<PermissionRecipientsState, PermissionRecipientsSideEffect>(
        initialState =
            PermissionRecipientsState(
                currentPermissions = alreadyAddedGroupPermissions.toList() + alreadyAddedUserPermissions.toList(),
            ),
    ) {
    private val alreadyAddedGroups = alreadyAddedGroupPermissions.toList()
    private val alreadyAddedUsers = alreadyAddedUserPermissions.toList()
    private val alreadyAddedGroupIds = alreadyAddedGroups.map { it.group.groupId }.toSet()
    private val alreadyAddedUserIds = alreadyAddedUsers.map { it.user.userId }.toSet()
    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadRecipients()
        observeSearchQuery()
    }

    fun onIntent(intent: PermissionRecipientsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is Search -> searchQueryChanged(intent.query)
            is ToggleGroupSelection -> toggleGroupSelection(intent.group)
            is ToggleUserSelection -> toggleUserSelection(intent.user)
            Save -> save()
        }
    }

    private fun loadRecipients() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            showRecipients(searchQuery = "")
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            searchQueryFlow
                .drop(1)
                .debounce(SEARCH_DEBOUNCE)
                .collectLatest { searchQuery ->
                    try {
                        showRecipients(searchQuery)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to search permission recipients")
                    }
                }
        }
    }

    private fun searchQueryChanged(searchQuery: String) {
        searchQueryFlow.value = searchQuery
        updateViewState {
            copy(searchInputEndIconMode = if (searchQuery.isNotBlank()) CLEAR else NONE)
        }
    }

    private suspend fun showRecipients(searchQuery: String) {
        val matchingGroups = getLocalGroupsUseCase.execute(GetLocalGroupsUseCase.Input(searchQuery)).groups
        val matchingUsers = getLocalUsersUseCase.execute(GetLocalUsersUseCase.Input(searchQuery)).users
        val filteredGroups = matchingGroups.filterNot { it.groupId in alreadyAddedGroupIds }
        val filteredUsers = matchingUsers.filterNot { it.id in alreadyAddedUserIds }
        val filteredExistingPermissions =
            if (searchQuery.isNotBlank()) {
                matchingExistingPermissions(matchingGroups, matchingUsers)
            } else {
                emptyList()
            }

        updateViewState {
            copy(
                filteredGroups = filteredGroups,
                filteredUsers = filteredUsers,
                filteredExistingPermissions = filteredExistingPermissions,
                showEmptyState =
                    searchQuery.isNotBlank() &&
                        filteredGroups.isEmpty() &&
                        filteredUsers.isEmpty() &&
                        filteredExistingPermissions.isEmpty(),
            )
        }
    }

    private fun matchingExistingPermissions(
        matchingGroups: List<GroupModel>,
        matchingUsers: List<UserUiModel>,
    ): List<PermissionModelUi> {
        val matchingGroupIds = matchingGroups.map { it.groupId }.toSet()
        val matchingUserIds = matchingUsers.map { it.id }.toSet()
        return alreadyAddedGroups.filter { it.group.groupId in matchingGroupIds } +
            alreadyAddedUsers.filter { it.user.userId in matchingUserIds }
    }

    private fun toggleGroupSelection(group: GroupModel) {
        val selectedGroups = viewState.value.selectedGroups
        val newSelectedGroups =
            if (group.groupId in selectedGroups) {
                selectedGroups - group.groupId
            } else {
                selectedGroups + (group.groupId to group)
            }
        updateViewState { copy(selectedGroups = newSelectedGroups) }
        recomputeDisplayPermissions()
    }

    private fun toggleUserSelection(user: UserUiModel) {
        val selectedUsers = viewState.value.selectedUsers
        val newSelectedUsers =
            if (user.id in selectedUsers) {
                selectedUsers - user.id
            } else {
                selectedUsers + (user.id to user)
            }
        updateViewState { copy(selectedUsers = newSelectedUsers) }
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
            state.selectedGroups.values
                .map {
                    permissionsModelMapper.map(
                        it,
                        DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS,
                        TEMPORARY_NEW_PERMISSION_ID,
                    )
                }
        val selectedUsers =
            state.selectedUsers.values
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
        val SEARCH_DEBOUNCE = 300.milliseconds
    }
}
