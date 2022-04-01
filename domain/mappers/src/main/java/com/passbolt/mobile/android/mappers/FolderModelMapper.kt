package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.FolderResponseDto
import com.passbolt.mobile.android.entity.resource.Folder
import com.passbolt.mobile.android.entity.resource.FolderWithChildItemsCount
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.FolderWithCount
import com.passbolt.mobile.android.ui.ResourcePermission

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
class FolderModelMapper {

    fun map(folder: FolderResponseDto): FolderModel =
        FolderModel(
            folderId = folder.id,
            parentFolderId = folder.folderParentId,
            name = folder.name.orEmpty(),
            isShared = folder.personal == false,
            permission = mapDtoPermissionTypeToUiModel(folder.permission.type)
        )

    fun map(folderModel: FolderModel): Folder =
        Folder(
            folderId = folderModel.folderId,
            name = folderModel.name,
            permission = folderModel.permission.toEntityModel(),
            parentId = folderModel.parentFolderId,
            isShared = folderModel.isShared
        )

    fun map(folderEntity: Folder): FolderModel =
        FolderModel(
            folderId = folderEntity.folderId,
            name = folderEntity.name,
            parentFolderId = folderEntity.parentId,
            isShared = folderEntity.isShared,
            permission = folderEntity.permission.toUiModel()
        )

    fun map(folderWithChildItemsCount: FolderWithChildItemsCount) =
        FolderWithCount(
            folderId = folderWithChildItemsCount.folderId,
            name = folderWithChildItemsCount.name,
            permission = folderWithChildItemsCount.permission.toUiModel(),
            parentId = folderWithChildItemsCount.parentId,
            isShared = folderWithChildItemsCount.isShared,
            subItemsCount = folderWithChildItemsCount.childItemsCount
        )

    private fun ResourcePermission.toEntityModel() = when (this) {
        ResourcePermission.READ -> Permission.READ
        ResourcePermission.UPDATE -> Permission.WRITE
        ResourcePermission.OWNER -> Permission.OWNER
    }
}
