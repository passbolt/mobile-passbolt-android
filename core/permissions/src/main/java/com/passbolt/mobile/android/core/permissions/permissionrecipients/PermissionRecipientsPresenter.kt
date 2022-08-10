package com.passbolt.mobile.android.core.permissions.permissionrecipients

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.permissions.PermissionsDatasetCreator
import com.passbolt.mobile.android.database.impl.groups.GetLocalGroupsUseCase
import com.passbolt.mobile.android.database.impl.users.GetLocalUsersUseCase
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper.Companion.TEMPORARY_NEW_PERMISSION_ID
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

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

class PermissionRecipientsPresenter(
    private val getLocalGroupsUseCase: GetLocalGroupsUseCase,
    private val getLocalUsersUseCase: GetLocalUsersUseCase,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val searchableMatcher: SearchableMatcher,
    coroutineLaunchContext: CoroutineLaunchContext
) :
    PermissionRecipientsContract.Presenter,
    BaseAuthenticatedPresenter<PermissionRecipientsContract.View>(coroutineLaunchContext) {

    override var view: PermissionRecipientsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private var alreadyAddedListWidth: Int = -1
    private var alreadyAddedItemWidth: Float = -1f
    private lateinit var alreadyAddedUsers: List<PermissionModelUi.UserPermissionModel>

    private val selectedPermissions = mutableListOf<PermissionModelUi>()
    private lateinit var alreadyAddedGroups: List<PermissionModelUi.GroupPermissionModel>
    private lateinit var groups: List<GroupModel>
    private lateinit var users: List<UserModel>

    /*
        Initial users and groups list has to be filtered - existing permissions have to be excluded from list.
        However existing permissions have to be added when users starts to search under separate "already added section"
     */
    override fun argsReceived(
        alreadyAddedGroupPermissions: List<PermissionModelUi.GroupPermissionModel>,
        alreadyAddedUserPermissions: List<PermissionModelUi.UserPermissionModel>,
        alreadyAddedListWidth: Int,
        alreadyAddedItemWidth: Float
    ) {
        this.alreadyAddedListWidth = alreadyAddedListWidth
        this.alreadyAddedItemWidth = alreadyAddedItemWidth
        this.alreadyAddedGroups = alreadyAddedGroupPermissions
        this.alreadyAddedUsers = alreadyAddedUserPermissions

        scope.launch {
            showPermissions(alreadyAddedGroupPermissions + alreadyAddedUserPermissions)

            groups = getLocalGroupsUseCase.execute(
                GetLocalGroupsUseCase.Input(
                    alreadyAddedGroupPermissions.map { it.group.groupId })
            ).groups

            users = getLocalUsersUseCase.execute(
                GetLocalUsersUseCase.Input(
                    alreadyAddedUserPermissions.map { it.user.userId })
            ).users

            view?.showRecipients(groups, users)
        }
    }

    override fun searchTextChange(searchText: String) {
        processSearchIconChange(searchText)
        view?.filterGroupsAndUsers(searchText)
    }

    /*
        User entered search query - show existing permissions additionally.
     */
    override fun groupsAndUsersItemsFiltered(constraint: String, resultsSize: Int) {
        val filteredExistingUsersAndGroups = (alreadyAddedGroups + alreadyAddedUsers)
            .filter { searchableMatcher.matches(it, constraint) }
        view?.showExistingUsersAndGroups(filteredExistingUsersAndGroups)
        if (resultsSize == 0 && filteredExistingUsersAndGroups.isEmpty()) {
            view?.showEmptyState()
        } else {
            view?.hideEmptyState()
        }
    }

    /*
        Search query is cleared - hide existing permissions.
     */
    override fun groupsAndUsersFilterReset() {
        view?.showExistingUsersAndGroups(emptyList())
    }

    private fun processSearchIconChange(searchText: String) {
        if (searchText.isNotBlank()) {
            view?.showClearSearchIcon()
        } else {
            view?.hideClearSearchIcon()
        }
    }

    private fun showPermissions(permissions: List<PermissionModelUi>) {
        val permissionsDisplayDataset = PermissionsDatasetCreator(
            alreadyAddedListWidth,
            alreadyAddedItemWidth
        )
            .prepareDataset(permissions)
        view?.showPermissions(
            permissionsDisplayDataset.groupPermissions,
            permissionsDisplayDataset.userPermissions,
            permissionsDisplayDataset.counterValue,
            permissionsDisplayDataset.overlap
        )
    }

    override fun groupRecipientSelectionChanged(model: GroupModel, isSelected: Boolean) {
        val permissionModel = permissionsModelMapper.map(
            model, DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS, TEMPORARY_NEW_PERMISSION_ID
        )
        processSelection(isSelected, permissionModel)
    }

    override fun userRecipientSelectionChanged(model: UserModel, isSelected: Boolean) {
        val permissionModel = permissionsModelMapper.map(
            model, DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS, TEMPORARY_NEW_PERMISSION_ID
        )
        processSelection(isSelected, permissionModel)
    }

    private fun processSelection(
        isSelected: Boolean,
        permissionModel: PermissionModelUi
    ) {
        if (isSelected) { // add permission to set
            selectedPermissions.add(permissionModel)
        } else { // remove permission from set
            val permissionRecipientId = when (permissionModel) {
                is PermissionModelUi.GroupPermissionModel -> permissionModel.group.groupId
                is PermissionModelUi.UserPermissionModel -> permissionModel.user.userId
            }
            val selectedItem = selectedPermissions.first {
                when (it) {
                    is PermissionModelUi.GroupPermissionModel -> it.group.groupId == permissionRecipientId
                    is PermissionModelUi.UserPermissionModel -> it.user.userId == permissionRecipientId
                }
            }
            selectedPermissions.remove(selectedItem)
        }
        showPermissions(alreadyAddedGroups + alreadyAddedUsers + selectedPermissions)
    }

    override fun searchClearClick() {
        view?.apply {
            hideEmptyState()
            clearSearch()
            hideClearSearchIcon()
        }
    }

    override fun saveButtonClick() {
        view?.apply {
            setSelectedPermissionsResult(alreadyAddedGroups + alreadyAddedUsers + selectedPermissions)
            navigateBack()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    companion object {
        @VisibleForTesting
        val DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS = ResourcePermission.READ
    }
}
