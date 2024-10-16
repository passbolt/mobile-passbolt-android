package com.passbolt.mobile.android.core.commonfolders.usecase.db

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.ui.PermissionModelUi

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

typealias ItemIdResourceId = GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input.ItemId.ResourceId
typealias ItemIdFolderId = GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input.ItemId.FolderId

class GetLocalParentFolderPermissionsToApplyToNewItemUseCase(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase
) : AsyncUseCase<GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input,
        GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Output> {

    /**
     * Gets folder permissions which are to be copied and applied to a newly created resource or folder in that folder.
     *
     * Permission IDs need to be replaced to a constant SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID
     */
    override suspend fun execute(input: Input): Output {
        val currentAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val currentAccountServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)

        val foldersDao = databaseProvider
            .get(currentAccount)
            .foldersDao()

        val resourcesDao = databaseProvider
            .get(currentAccount)
            .resourcesDao()

        val groupsPermissions = foldersDao.getFolderGroupsPermissions(input.parentFolderId)
            .map { it.copy(permissionId = SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID) }
        val usersPermissions = foldersDao.getFolderUsersPermissions(input.parentFolderId)
            .map {
                if (it.userId != currentAccountServerId) {
                    // new permissions from parent folder to inherit
                    it.copy(permissionId = SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID)
                } else {
                    // special case: permission for current user
                    //
                    // use permission values from parent folder to apply but overwrite the permission id
                    val currentUserPermissions = when (val itemId = input.itemId) {
                        is Input.ItemId.FolderId -> foldersDao.getFolderUsersPermissions(itemId.folderId)
                        is Input.ItemId.ResourceId -> resourcesDao.getResourceUsersPermissions(itemId.resourceId)
                    }

                    require(currentUserPermissions.size == 1) {
                        "On newly created item there should be exactly one permission" +
                                " - only for the current user (the one who just created the item)"
                    }
                    it.copy(permissionId = currentUserPermissions[0].permissionId)
                }
            }

        return Output(
            permissionsModelMapper.map(
                groupsPermissions,
                usersPermissions
            )
        )
    }

    data class Input(
        val parentFolderId: String,
        val itemId: ItemId
    ) {

        sealed class ItemId {

            data class ResourceId(val resourceId: String) : ItemId()

            data class FolderId(val folderId: String) : ItemId()
        }
    }

    data class Output(
        val permissions: List<PermissionModelUi>
    )
}
