package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.FolderResponseDto
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderWithChildItemsCountAndPath
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.FolderModelWithAttributes
import com.passbolt.mobile.android.ui.FolderWithCountAndPath

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
class FolderModelMapper(
    private val permissionsModelMapper: PermissionsModelMapper
) {

    fun map(folder: FolderResponseDto): FolderModelWithAttributes =
        FolderModelWithAttributes(
            FolderModel(
                folderId = folder.id,
                parentFolderId = folder.folderParentId,
                name = folder.name.orEmpty(),
                isShared = folder.personal == false,
                permission = permissionsModelMapper.map(folder.permission.type)
            ),
            folder.permissions.map(permissionsModelMapper::map)
        )

    fun map(folderModel: FolderModel): Folder =
        Folder(
            folderId = folderModel.folderId,
            name = folderModel.name,
            permission = permissionsModelMapper.map(folderModel.permission),
            parentId = folderModel.parentFolderId,
            isShared = folderModel.isShared
        )

    fun map(folderEntity: Folder): FolderModel =
        FolderModel(
            folderId = folderEntity.folderId,
            name = folderEntity.name,
            parentFolderId = folderEntity.parentId,
            isShared = folderEntity.isShared,
            permission = permissionsModelMapper.map(folderEntity.permission)
        )

    fun map(folderWithChildItemsCountAndPath: FolderWithChildItemsCountAndPath) =
        FolderWithCountAndPath(
            folderId = folderWithChildItemsCountAndPath.folderId,
            name = folderWithChildItemsCountAndPath.name,
            permission = permissionsModelMapper.map(folderWithChildItemsCountAndPath.permission),
            parentId = folderWithChildItemsCountAndPath.parentId,
            isShared = folderWithChildItemsCountAndPath.isShared,
            subItemsCount = folderWithChildItemsCountAndPath.childItemsCount,
            path = folderWithChildItemsCountAndPath.path
        )
}
