package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

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
 * Model of resource used to parse backend response.
 * @param resourceTypeId id of the type of the resource (secret, secret with description and more in future)
 * @param resourceFolderId id of the resource folder or null if in root
 * @param favorite if resource is in favourites it contains a favourite object else null
 */
data class ResourceResponseDto(
    val id: String,
    @SerializedName("resource_type_id")
    val resourceTypeId: String,
    @SerializedName("folder_parent_id")
    val resourceFolderId: String?,
    val description: String?,
    val name: String,
    val uri: String?,
    val username: String?,
    val permission: PermissionDto,
    val favorite: JSONObject?,
    val modified: String
)
