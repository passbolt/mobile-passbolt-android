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
object Migration27to28 : Migration(27, 28) {
    private const val CREATE_INDEX_FOLDER_PARENT_ID =
        "CREATE INDEX IF NOT EXISTS `index_Folder_parentId` ON `Folder` (`parentId`)"
    private const val CREATE_INDEX_FOLDER_MODIFIED_FOLDER_ID =
        "CREATE INDEX IF NOT EXISTS `index_Folder_modified_folderId` ON `Folder` (`modified` DESC, `folderId` ASC)"
    private const val CREATE_INDEX_RESOURCE_MODIFIED_RESOURCE_ID =
        "CREATE INDEX IF NOT EXISTS `index_Resource_modified_resourceId` ON `Resource` (`modified` DESC, `resourceId` ASC)"
    private const val CREATE_INDEX_RESOURCE_EXPIRY_RESOURCE_ID =
        "CREATE INDEX IF NOT EXISTS `index_Resource_expiry_resourceId` ON `Resource` (`expiry`, `resourceId`)"
    private const val CREATE_INDEX_RESOURCE_FAVOURITE_ID =
        "CREATE INDEX IF NOT EXISTS `index_Resource_favouriteId` ON `Resource` (`favouriteId`)"

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_INDEX_FOLDER_PARENT_ID)
        connection.execSQL(CREATE_INDEX_FOLDER_MODIFIED_FOLDER_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_MODIFIED_RESOURCE_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_EXPIRY_RESOURCE_ID)
        connection.execSQL(CREATE_INDEX_RESOURCE_FAVOURITE_ID)
    }
}
