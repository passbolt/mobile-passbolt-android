package com.passbolt.mobile.android.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Suppress("MagicNumber")
object Migration17to18 : Migration(17, 18) {

    private const val ADD_METADATA_PRIVATE_KEY_PASSPHRASE =
        "ALTER TABLE MetadataPrivateKey ADD COLUMN `passphrase` TEXT NOT NULL DEFAULT ''"

    private const val DROP_RESOURCES = "DROP TABLE Resource"

    @Suppress("MaxLineLength")
    private const val CREATE_RESOURCES = "CREATE TABLE IF NOT EXISTS `Resource` (" +
            "`resourceId` TEXT NOT NULL, " +
            "`folderId` TEXT, " +
            "`resourcePermission` TEXT NOT NULL, " +
            "`resourceTypeId` TEXT NOT NULL, " +
            "`favouriteId` TEXT, " +
            "`modified` INTEGER NOT NULL, " +
            "`expiry` INTEGER, " +
            "`metadataKeyId` TEXT, " +
            "`metadataKeyType` TEXT, " +
            "PRIMARY KEY(`resourceId`), " +
            "FOREIGN KEY(`folderId`) REFERENCES `Folder`(`folderId`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
            "FOREIGN KEY(`resourceTypeId`) REFERENCES `ResourceType`(`resourceTypeId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            execSQL(ADD_METADATA_PRIVATE_KEY_PASSPHRASE)
            execSQL(DROP_RESOURCES)
            execSQL(CREATE_RESOURCES)
        }
    }
}
