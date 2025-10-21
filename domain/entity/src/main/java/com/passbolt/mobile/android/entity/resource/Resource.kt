package com.passbolt.mobile.android.entity.resource

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.PrimaryKey
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.metadata.MetadataKeyType
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

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = SET_NULL,
        ),
        ForeignKey(
            entity = ResourceType::class,
            parentColumns = ["resourceTypeId"],
            childColumns = ["resourceTypeId"],
            onDelete = CASCADE,
        ),
    ],
)
data class Resource(
    @PrimaryKey
    val resourceId: String,
    val folderId: String?,
    val resourcePermission: Permission,
    val resourceTypeId: String,
    val favouriteId: String?,
    val modified: ZonedDateTime,
    val expiry: ZonedDateTime?,
    val metadataKeyId: String?,
    val metadataKeyType: MetadataKeyType?,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE,
        ),
    ],
)
data class ResourceMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val resourceId: String,
    val metadataJson: String,
    val name: String,
    val username: String?,
    val description: String?,
    val customFieldsKeys: String?,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE,
        ),
    ],
)
data class ResourceUri(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val resourceId: String,
    val uri: String,
)

data class ResourceWithMetadata(
    @PrimaryKey
    val resourceId: String,
    val folderId: String?,
    val resourcePermission: Permission,
    val resourceTypeId: String,
    val favouriteId: String?,
    val modified: ZonedDateTime,
    val expiry: ZonedDateTime?,
    val metadataJson: String,
    val metadataKeyId: String?,
    val metadataKeyType: MetadataKeyType?,
)
