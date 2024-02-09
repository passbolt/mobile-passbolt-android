package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.ui.ResourceModel
import java.time.ZonedDateTime

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
    private val initialsProvider: InitialsProvider,
    private val permissionsModelMapper: PermissionsModelMapper
) {

    fun map(resource: ResourceResponseDto): ResourceModel =
        ResourceModel(
            resourceId = resource.id.toString(),
            resourceTypeId = resource.resourceTypeId.toString(),
            folderId = resource.resourceFolderId?.toString(),
            name = resource.name,
            username = resource.username,
            icon = null,
            initials = initialsProvider.get(resource.name),
            url = resource.uri,
            description = resource.description,
            permission = permissionsModelMapper.map(resource.permission.type),
            favouriteId = resource.favorite?.id?.toString(),
            modified = ZonedDateTime.parse(resource.modified),
            expiry = resource.expired?.let { ZonedDateTime.parse(it) }
        )

    fun map(resourceModel: ResourceModel): Resource =
        Resource(
            resourceId = resourceModel.resourceId,
            folderId = resourceModel.folderId,
            resourceName = resourceModel.name,
            description = resourceModel.description,
            resourcePermission = permissionsModelMapper.map(resourceModel.permission),
            url = resourceModel.url,
            username = resourceModel.username,
            resourceTypeId = resourceModel.resourceTypeId,
            favouriteId = resourceModel.favouriteId,
            modified = resourceModel.modified,
            expiry = resourceModel.expiry
        )

    fun map(resourceEntity: Resource): ResourceModel =
        ResourceModel(
            resourceId = resourceEntity.resourceId,
            resourceTypeId = resourceEntity.resourceTypeId,
            folderId = resourceEntity.folderId,
            name = resourceEntity.resourceName,
            username = resourceEntity.username,
            icon = null,
            initials = initialsProvider.get(resourceEntity.resourceName),
            url = resourceEntity.url,
            description = resourceEntity.description,
            permission = permissionsModelMapper.map(resourceEntity.resourcePermission),
            favouriteId = resourceEntity.favouriteId,
            modified = resourceEntity.modified,
            expiry = resourceEntity.expiry
        )
}
