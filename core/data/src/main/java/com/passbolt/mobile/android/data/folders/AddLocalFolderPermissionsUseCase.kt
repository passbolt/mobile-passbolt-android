package com.passbolt.mobile.android.data.folders

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.entity.folder.FolderAndUsersCrossRef
import com.passbolt.mobile.android.entity.group.FolderAndGroupsCrossRef
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.ui.FolderModelWithAttributes
import com.passbolt.mobile.android.ui.PermissionModel

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
class AddLocalFolderPermissionsUseCase(
    private val databaseProvider: DatabaseProvider,
    private val permissionsModelMapper: PermissionsModelMapper
) : AsyncUseCase<AddLocalFolderPermissionsUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val db = databaseProvider.get(input.userId)
        val foldersAndGroupsCrossRefDao = db.folderAndGroupsCrossRefDao()
        val foldersAndUsersCrossRefDao = db.folderAndUsersCrossRefDao()

        input.foldersWithAttributes
            .map { it.folderModel.folderId to it.folderPermissions }
            .forEach { (folderId, permissions) ->
                permissions.forEach { permission ->
                    when (permission) {
                        is PermissionModel.GroupPermissionModel ->
                            foldersAndGroupsCrossRefDao.insert(
                                FolderAndGroupsCrossRef(
                                    folderId,
                                    permission.group.groupId,
                                    permissionsModelMapper.map(permission.permission),
                                    permission.permissionId
                                )
                            )
                        is PermissionModel.UserPermissionModel -> {
                            foldersAndUsersCrossRefDao.insert(
                                FolderAndUsersCrossRef(
                                    folderId,
                                    permission.userId,
                                    permissionsModelMapper.map(permission.permission),
                                    permission.permissionId
                                )
                            )
                        }
                    }
                }
            }
    }

    data class Input(
        val foldersWithAttributes: List<FolderModelWithAttributes>,
        val userId: String
    )
}
