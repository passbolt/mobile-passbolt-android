package com.passbolt.mobile.android.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

@Suppress("MagicNumber")
object Migration23to24 : Migration(23, 24) {
    private const val CREATE_INDEX_RESOURCE_FOLDER_ID =
        "CREATE INDEX IF NOT EXISTS `index_Resource_folderId` ON `Resource` (`folderId`)"
    private const val CREATE_INDEX_RESOURCE_RESOURCE_TYPE_ID =
        "CREATE INDEX IF NOT EXISTS `index_Resource_resourceTypeId` ON `Resource` (`resourceTypeId`)"
    private const val CREATE_INDEX_RESOURCE_AND_TAGS_CROSS_REF_RESOURCE_ID =
        "CREATE INDEX IF NOT EXISTS `index_ResourceAndTagsCrossRef_resourceId` ON `ResourceAndTagsCrossRef` (`resourceId`)"
    private const val CREATE_INDEX_RESOURCE_AND_GROUPS_CROSS_REF_GROUP_ID =
        "CREATE INDEX IF NOT EXISTS `index_ResourceAndGroupsCrossRef_groupId` ON `ResourceAndGroupsCrossRef` (`groupId`)"
    private const val CREATE_INDEX_USERS_AND_GROUP_CROSS_REF_GROUP_ID =
        "CREATE INDEX IF NOT EXISTS `index_UsersAndGroupCrossRef_groupId` ON `UsersAndGroupCrossRef` (`groupId`)"
    private const val CREATE_INDEX_RESOURCE_AND_USERS_CROSS_REF_USER_ID =
        "CREATE INDEX IF NOT EXISTS `index_ResourceAndUsersCrossRef_userId` ON `ResourceAndUsersCrossRef` (`userId`)"
    private const val CREATE_INDEX_FOLDER_AND_USERS_CROSS_REF_FOLDER_ID =
        "CREATE INDEX IF NOT EXISTS `index_FolderAndUsersCrossRef_folderId` ON `FolderAndUsersCrossRef` (`folderId`)"
    private const val CREATE_INDEX_FOLDER_AND_GROUPS_CROSS_REF_GROUP_ID =
        "CREATE INDEX IF NOT EXISTS `index_FolderAndGroupsCrossRef_groupId` ON `FolderAndGroupsCrossRef` (`groupId`)"
    private const val CREATE_INDEX_RESOURCE_URI_RESOURCE_ID =
        "CREATE INDEX IF NOT EXISTS `index_ResourceUri_resourceId` ON `ResourceUri` (`resourceId`)"
    private const val CREATE_INDEX_METADATA_PRIVATE_KEY_METADATA_KEY_ID =
        "CREATE INDEX IF NOT EXISTS `index_MetadataPrivateKey_metadataKeyId` ON `MetadataPrivateKey` (`metadataKeyId`)"

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_INDEX_RESOURCE_FOLDER_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_RESOURCE_TYPE_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_AND_TAGS_CROSS_REF_RESOURCE_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_AND_GROUPS_CROSS_REF_GROUP_ID)
        connection.execSQL(CREATE_INDEX_USERS_AND_GROUP_CROSS_REF_GROUP_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_AND_USERS_CROSS_REF_USER_ID)
        connection.execSQL(CREATE_INDEX_FOLDER_AND_USERS_CROSS_REF_FOLDER_ID)
        connection.execSQL(CREATE_INDEX_FOLDER_AND_GROUPS_CROSS_REF_GROUP_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_URI_RESOURCE_ID)
        connection.execSQL(CREATE_INDEX_METADATA_PRIVATE_KEY_METADATA_KEY_ID)
    }
}
