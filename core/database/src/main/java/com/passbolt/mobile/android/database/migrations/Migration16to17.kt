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
object Migration16to17 : Migration(16, 17) {
    private const val DROP_RESOURCE_TYPE_FIELDS_CROSS_REF = "DROP TABLE ResourceTypesAndFieldsCrossRef"
    private const val DROP_RESOURCE_TYPE_FIELDS = "DROP TABLE ResourceField"
    private const val DROP_RESOURCE_TYPE = "DROP TABLE ResourceType"
    private const val DROP_RESOURCES = "DROP TABLE Resource"

    @Suppress("MaxLineLength")
    private const val CREATE_RESOURCES =
        "CREATE TABLE IF NOT EXISTS Resource (" +
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
            "FOREIGN KEY(`resourceTypeId`) REFERENCES `ResourceType`(`resourceTypeId`) ON UPDATE NO ACTION ON DELETE NO ACTION " +
            ")"
    private const val CREATE_METADATA_KEYS =
        "CREATE TABLE IF NOT EXISTS MetadataKey (" +
            "`id` TEXT NOT NULL, " +
            "`fingerprint` TEXT, " +
            "`armoredKey` TEXT NOT NULL, " +
            "`expired` INTEGER, " +
            "`deleted` INTEGER, " +
            "PRIMARY KEY(`id`)" +
            ")"
    private const val CREATE_METADATA_PRIVATE_KEYS =
        "CREATE TABLE IF NOT EXISTS MetadataPrivateKey (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`metadataKeyId` TEXT NOT NULL, " +
            "`userId` TEXT, " +
            "`data` TEXT NOT NULL, " +
            "FOREIGN KEY(`metadataKeyId`) REFERENCES `MetadataKey`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE " +
            ")"
    private const val CREATE_RESOURCE_TYPE =
        "CREATE TABLE IF NOT EXISTS ResourceType (" +
            "`resourceTypeId` TEXT NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`slug` TEXT NOT NULL, " +
            "`deleted` INTEGER, " +
            "PRIMARY KEY(`resourceTypeId`)" +
            ")"
    private const val ADD_USER_KEY_ID_COLUMN = "ALTER TABLE User ADD COLUMN `userKeyId` TEXT NOT NULL DEFAULT ''"

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            execSQL(DROP_RESOURCE_TYPE_FIELDS_CROSS_REF)
            execSQL(DROP_RESOURCE_TYPE_FIELDS)
            execSQL(DROP_RESOURCE_TYPE)
            execSQL(CREATE_RESOURCE_TYPE)
            execSQL(DROP_RESOURCES)
            execSQL(CREATE_RESOURCES)
            execSQL(CREATE_METADATA_KEYS)
            execSQL(CREATE_METADATA_PRIVATE_KEYS)
            execSQL(ADD_USER_KEY_ID_COLUMN)
        }
    }
}
