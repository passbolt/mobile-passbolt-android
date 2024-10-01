package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

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

/**
 * A compatibility resource model - can eiter support v4 resource or v5 resource with metadata.
 */
sealed class ResourceResponseDto(
    val id: UUID,
    @SerializedName("resource_type_id")
    val resourceTypeId: UUID,
    @SerializedName("folder_parent_id")
    val resourceFolderId: UUID?,
    val permission: PermissionDto,
    val favorite: FavouriteDto?,
    val modified: String,
    val tags: List<TagDto>?,
    val expired: String?,
    val permissions: List<PermissionWithGroupDto>?
)

class ResourceResponseV4Dto(
    val description: String?,
    val name: String,
    val uri: String?,
    val username: String?,
    id: UUID,
    resourceTypeId: UUID,
    resourceFolderId: UUID?,
    permission: PermissionDto,
    favorite: FavouriteDto?,
    modified: String,
    tags: List<TagDto>?,
    expired: String?,
    permissions: List<PermissionWithGroupDto>?
) : ResourceResponseDto(
    id, resourceTypeId, resourceFolderId, permission, favorite, modified, tags, expired, permissions
)

class ResourceResponseV5Dto(
    val metadata: String,
    id: UUID,
    resourceTypeId: UUID,
    resourceFolderId: UUID?,
    permission: PermissionDto,
    favorite: FavouriteDto?,
    modified: String,
    tags: List<TagDto>?,
    expired: String?,
    permissions: List<PermissionWithGroupDto>?
) : ResourceResponseDto(
    id, resourceTypeId, resourceFolderId, permission, favorite, modified, tags, expired, permissions
)

data class TagDto(
    val id: UUID,
    val slug: String,
    @SerializedName("is_shared")
    val isShared: Boolean
)

data class FavouriteDto(
    val id: UUID
)
