package com.passbolt.mobile.android.mappers

import com.google.gson.JsonObject
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto.PERSONAL
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto.SHARED
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.dto.response.ResourceResponseV4Dto
import com.passbolt.mobile.android.dto.response.ResourceResponseV5Dto
import com.passbolt.mobile.android.entity.metadata.MetadataKeyType
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceMetadata
import com.passbolt.mobile.android.entity.resource.ResourceUri
import com.passbolt.mobile.android.entity.resource.ResourceWithMetadata
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
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
    private val permissionsModelMapper: PermissionsModelMapper
) {

    fun map(resource: ResourceResponseDto): ResourceModel = when (resource) {
        is ResourceResponseV4Dto -> {
            ResourceModel(
                resourceId = resource.id.toString(),
                resourceTypeId = resource.resourceTypeId.toString(),
                folderId = resource.resourceFolderId?.toString(),
                permission = permissionsModelMapper.map(resource.permission.type),
                favouriteId = resource.favorite?.id?.toString(),
                modified = ZonedDateTime.parse(resource.modified),
                expiry = resource.expired?.let { ZonedDateTime.parse(it) },
                metadataJsonModel = getV5Metadata(resource),
                metadataKeyId = null,
                metadataKeyType = null
            )
        }
        is ResourceResponseV5Dto -> {
            ResourceModel(
                resourceId = resource.id.toString(),
                resourceTypeId = resource.resourceTypeId.toString(),
                folderId = resource.resourceFolderId?.toString(),
                permission = permissionsModelMapper.map(resource.permission.type),
                favouriteId = resource.favorite?.id?.toString(),
                modified = ZonedDateTime.parse(resource.modified),
                expiry = resource.expired?.let { ZonedDateTime.parse(it) },
                metadataJsonModel = MetadataJsonModel(resource.metadata),
                metadataKeyId = resource.metadataKeyId.toString(),
                metadataKeyType = map(resource.metadataKeyType)
            )
        }
    }

    private fun map(metadataKeyTypeDto: MetadataKeyTypeDto) = when (metadataKeyTypeDto) {
        SHARED -> MetadataKeyTypeModel.SHARED
        PERSONAL -> MetadataKeyTypeModel.PERSONAL
    }

    private fun map(metadataKeyTypeModel: MetadataKeyTypeModel?) = when (metadataKeyTypeModel) {
        MetadataKeyTypeModel.SHARED -> MetadataKeyType.SHARED
        MetadataKeyTypeModel.PERSONAL -> MetadataKeyType.PERSONAL
        null -> null
    }

    private fun map(metadataKeyType: MetadataKeyType?) = when (metadataKeyType) {
        MetadataKeyType.SHARED -> MetadataKeyTypeModel.SHARED
        MetadataKeyType.PERSONAL -> MetadataKeyTypeModel.PERSONAL
        null -> null
    }

    fun map(resourceModel: ResourceModel): Resource =
        Resource(
            resourceId = resourceModel.resourceId,
            folderId = resourceModel.folderId,
            resourcePermission = permissionsModelMapper.map(resourceModel.permission),
            resourceTypeId = resourceModel.resourceTypeId,
            favouriteId = resourceModel.favouriteId,
            modified = resourceModel.modified,
            expiry = resourceModel.expiry,
            metadataKeyId = resourceModel.metadataKeyId,
            metadataKeyType = map(resourceModel.metadataKeyType)
        )

    fun mapResourceMetadata(resourceModel: ResourceModel): ResourceMetadata =
        ResourceMetadata(
            resourceId = resourceModel.resourceId,
            metadataJson = resourceModel.metadataJsonModel.json,
            name = resourceModel.metadataJsonModel.name,
            username = resourceModel.metadataJsonModel.username,
            description = resourceModel.metadataJsonModel.description
        )

    fun mapResourceUri(resourceModel: ResourceModel) = resourceModel.metadataJsonModel.uri?.let {
        ResourceUri(resourceId = resourceModel.resourceId, uri = it)
    }

    fun map(resourceEntity: ResourceWithMetadata): ResourceModel =
        ResourceModel(
            resourceId = resourceEntity.resourceId,
            resourceTypeId = resourceEntity.resourceTypeId,
            folderId = resourceEntity.folderId,
            permission = permissionsModelMapper.map(resourceEntity.resourcePermission),
            favouriteId = resourceEntity.favouriteId,
            modified = resourceEntity.modified,
            expiry = resourceEntity.expiry,
            metadataJsonModel = MetadataJsonModel(resourceEntity.metadataJson),
            metadataKeyId = resourceEntity.metadataKeyId,
            metadataKeyType = map(resourceEntity.metadataKeyType)
        )

    private fun getV5Metadata(resourceModel: ResourceResponseV4Dto) =
        MetadataJsonModel(
            JsonObject().apply {
                addProperty("name", resourceModel.name)
                addProperty("username", resourceModel.username)
                addProperty("description", resourceModel.description)
                addProperty("uri", resourceModel.uri)
            }.toString()
        )
}
