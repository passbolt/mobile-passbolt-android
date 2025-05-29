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

package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * A compatibility resource model - can eiter support v4 resource or v5 resource with metadata.
 */
sealed class ResourceResponseDto {
    abstract val id: UUID
    abstract val resourceTypeId: UUID
    abstract val resourceFolderId: UUID?
    abstract val permission: PermissionDto
    abstract val favorite: FavouriteDto?
    abstract val modified: String
    abstract val tags: List<TagDto>?
    abstract val expired: String?
    abstract val permissions: List<PermissionWithGroupDto>?
}

data class ResourceResponseV4Dto(
    val description: String?,
    val name: String,
    val uri: String?,
    val username: String?,
    override val id: UUID,
    @SerializedName("resource_type_id")
    override val resourceTypeId: UUID,
    @SerializedName("folder_parent_id")
    override val resourceFolderId: UUID?,
    override val permission: PermissionDto,
    override val favorite: FavouriteDto?,
    override val modified: String,
    override val tags: List<TagDto>?,
    override val expired: String?,
    override val permissions: List<PermissionWithGroupDto>?,
) : ResourceResponseDto()

data class ResourceResponseV5Dto(
    val metadata: String,
    @SerializedName("metadata_key_id")
    val metadataKeyId: UUID,
    @SerializedName("metadata_key_type")
    val metadataKeyType: MetadataKeyTypeDto,
    override val id: UUID,
    @SerializedName("resource_type_id")
    override val resourceTypeId: UUID,
    @SerializedName("folder_parent_id")
    override val resourceFolderId: UUID?,
    override val permission: PermissionDto,
    override val favorite: FavouriteDto?,
    override val modified: String,
    override val tags: List<TagDto>?,
    override val expired: String?,
    override val permissions: List<PermissionWithGroupDto>?,
) : ResourceResponseDto()

enum class MetadataKeyTypeDto {
    @SerializedName("shared_key")
    SHARED,

    @SerializedName("user_key")
    PERSONAL,
}

data class TagDto(
    val id: UUID,
    val slug: String,
    @SerializedName("is_shared")
    val isShared: Boolean,
)

data class FavouriteDto(
    val id: UUID,
)
