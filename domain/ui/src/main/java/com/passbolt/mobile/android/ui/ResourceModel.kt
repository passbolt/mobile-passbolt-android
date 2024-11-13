package com.passbolt.mobile.android.ui

import android.os.Parcelable
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.Searchable
import com.passbolt.mobile.android.jsonmodel.JsonModel
import com.passbolt.mobile.android.jsonmodel.delegates.RootRelativeJsonPathNullableStringDelegate
import com.passbolt.mobile.android.jsonmodel.delegates.RootRelativeJsonPathNullableStringListDelegate
import com.passbolt.mobile.android.jsonmodel.delegates.RootRelativeJsonPathStringDelegate
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.java.KoinJavaComponent.inject
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

@Parcelize
data class ResourceModel(
    val resourceId: String,
    val resourceTypeId: String,
    val folderId: String?,
    val permission: ResourcePermission,
    val favouriteId: String?,
    val modified: ZonedDateTime,
    val expiry: ZonedDateTime?,
    val metadataKeyId: String?,
    val metadataKeyType: MetadataKeyTypeModel?,
    override var json: String
) : Parcelable, Searchable, JsonModel {

    @IgnoredOnParcel
    var name: String by RootRelativeJsonPathStringDelegate(jsonPath = "name")

    @IgnoredOnParcel
    var username: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "username")

    @IgnoredOnParcel
    var description: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "description")

    @IgnoredOnParcel
    var uri: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "uri")

    @IgnoredOnParcel
    var uris: List<String>? by RootRelativeJsonPathNullableStringListDelegate(jsonPath = "uris")

    @IgnoredOnParcel
    val initials: String
        get() = initialsProvider.get(name)

    @IgnoredOnParcel
    override val searchCriteria: String = "$name${username.orEmpty()}${uri.orEmpty()}${uris?.joinToString()}"

    companion object {
        val initialsProvider: InitialsProvider by inject(InitialsProvider::class.java)
    }
}

fun ResourceModel.isFavourite() = favouriteId != null

data class ResourceModelWithAttributes(
    val resourceModel: ResourceModel,
    val resourceTags: List<TagModel>,
    val resourcePermissions: List<PermissionModel>,
    val favouriteId: String?
)

enum class MetadataKeyTypeModel {
    SHARED,
    PERSONAL
}

open class CreateResourceModel(
    val contentType: ContentType,
    val folderId: String?,
    val expiry: ZonedDateTime?,
    val metadataKeyId: String?,
    val metadataKeyType: MetadataKeyTypeModel?,
    override var json: String
) : JsonModel {

    var objectType: String by RootRelativeJsonPathStringDelegate(jsonPath = "object_type")

    var resourceTypeId: String by RootRelativeJsonPathStringDelegate(jsonPath = "resource_type_id")

    var name: String by RootRelativeJsonPathStringDelegate(jsonPath = "name")

    var username: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "username")

    var description: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "description")

    var uri: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "uri")

    var uris: List<String>? by RootRelativeJsonPathNullableStringListDelegate(jsonPath = "uris")
}

class UpdateResourceModel(
    val resourceId: String,
    contentType: ContentType,
    folderId: String?,
    expiry: ZonedDateTime?,
    metadataKeyId: String?,
    metadataKeyType: MetadataKeyTypeModel?,
    json: String
) : CreateResourceModel(
    contentType = contentType,
    folderId = folderId,
    expiry = expiry,
    metadataKeyId = metadataKeyId,
    metadataKeyType = metadataKeyType,
    json = json
)
