package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.entity.resource.Folder
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.ui.ResourceModel
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
class ResourceModelMapper(
    private val initialsProvider: InitialsProvider
) {

    fun map(resource: ResourceResponseDto): ResourceModel =
        ResourceModel(
            resourceId = resource.id,
            resourceTypeId = resource.resourceTypeId,
            name = resource.name,
            username = resource.username,
            icon = null,
            initials = initialsProvider.get(resource.name),
            url = resource.uri,
            description = resource.description,
            permission = mapDtoPermissionToUiModel(resource.permission.type),
            isFavourite = resource.favorite != null
        )

    fun map(resourceModel: ResourceModel): Resource =
        Resource(
            resourceId = resourceModel.resourceId,
            resourceName = resourceModel.name,
            description = resourceModel.description,
            resourcePermission = resourceModel.permission.toEntityModel(),
            url = resourceModel.url,
            username = resourceModel.username,
            resourceTypeId = resourceModel.resourceTypeId,
            folder = Folder(name = "name", permission = Permission.READ, parentId = 0),
            isFavourite = resourceModel.isFavourite
        )

    fun map(resourceEntity: Resource): ResourceModel =
        ResourceModel(
            resourceId = resourceEntity.resourceId,
            resourceTypeId = resourceEntity.resourceTypeId,
            name = resourceEntity.resourceName,
            username = resourceEntity.username,
            icon = null,
            initials = initialsProvider.get(resourceEntity.resourceName),
            url = resourceEntity.url,
            description = resourceEntity.description,
            permission = resourceEntity.resourcePermission.toUiModel(),
            isFavourite = resourceEntity.isFavourite
        )

    private fun ResourcePermission.toEntityModel() = when (this) {
        ResourcePermission.READ -> Permission.READ
        ResourcePermission.UPDATE -> Permission.WRITE
        ResourcePermission.OWNER -> Permission.OWNER
    }

    @Suppress("MagicNumber")
    private fun mapDtoPermissionToUiModel(type: Int) = when (type) {
        1 -> ResourcePermission.READ
        7 -> ResourcePermission.UPDATE
        15 -> ResourcePermission.OWNER
        else -> throw IllegalArgumentException("Unsupported DTO permission value: $this")
    }

    private fun Permission.toUiModel() = when (this) {
        Permission.READ -> ResourcePermission.READ
        Permission.WRITE -> ResourcePermission.UPDATE
        Permission.OWNER -> ResourcePermission.OWNER
    }
}
