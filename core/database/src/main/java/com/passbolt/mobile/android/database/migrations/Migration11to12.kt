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

@Suppress("MagicNumber", "MaxLineLength")
object Migration11to12 : Migration(11, 12) {

    private val TABLES_TO_DROP = listOf(
        "ResourceAndTagsCrossRef",
        "ResourceAndUsersCrossRef",
        "ResourceAndGroupsCrossRef",
        "FolderAndUsersCrossRef",
        "ResourceTypesAndFieldsCrossRef",
        "UsersAndGroupCrossRef",
        "FolderAndGroupsCrossRef",
        "Resource"
    )

    private const val CREATE_RESOURCE_TABLE =
        "CREATE TABLE IF NOT EXISTS `Resource` (" +
                "`resourceId` TEXT NOT NULL, " +
                "`folderId` TEXT, " +
                "`resourceName` TEXT NOT NULL, " +
                "`resourcePermission` TEXT NOT NULL, " +
                "`url` TEXT, `username` TEXT, " +
                "`description` TEXT, " +
                "`resourceTypeId` TEXT NOT NULL, " +
                "`favouriteId` TEXT, " +
                "`modified` INTEGER NOT NULL, " +
                "PRIMARY KEY(`resourceId`), " +
                "FOREIGN KEY(`folderId`) REFERENCES `Folder`(`folderId`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                "FOREIGN KEY(`resourceTypeId`) REFERENCES `ResourceType`(`resourceTypeId`) ON UPDATE NO ACTION ON DELETE NO ACTION )"

    private const val CREATE_RESOURCE_TYPES_AND_FIELDS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `ResourceTypesAndFieldsCrossRef` (" +
                "`resourceTypeId` TEXT NOT NULL, " +
                "`resourceFieldId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`resourceTypeId`, `resourceFieldId`), " +
                "FOREIGN KEY(`resourceTypeId`) REFERENCES `ResourceType`(`resourceTypeId`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`resourceFieldId`) REFERENCES `ResourceField`(`resourceFieldId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val CREATE_RESOURCE_AND_TAGS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `ResourceAndTagsCrossRef` (" +
                "`tagId` TEXT NOT NULL, " +
                "`resourceId` TEXT NOT NULL, " +
                "PRIMARY KEY(`tagId`, `resourceId`), " +
                "FOREIGN KEY(`tagId`) REFERENCES `Tag`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`resourceId`) REFERENCES `Resource`(`resourceId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val CREATE_RESOURCE_AND_GROUPS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `ResourceAndGroupsCrossRef` (" +
                "`resourceId` TEXT NOT NULL, " +
                "`groupId` TEXT NOT NULL, " +
                "`permission` TEXT NOT NULL, " +
                "`permissionId` TEXT NOT NULL, " +
                "PRIMARY KEY(`resourceId`, `groupId`), " +
                "FOREIGN KEY(`resourceId`) REFERENCES `Resource`(`resourceId`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`groupId`) REFERENCES `UsersGroup`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val CREATE_USERS_AND_GROUPS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `UsersAndGroupCrossRef` (" +
                "`userId` TEXT NOT NULL, " +
                "`groupId` TEXT NOT NULL, " +
                "PRIMARY KEY(`userId`, `groupId`), " +
                "FOREIGN KEY(`userId`) REFERENCES `User`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`groupId`) REFERENCES `UsersGroup`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val CREATE_RESOURCE_AND_USERS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `ResourceAndUsersCrossRef` (" +
                "`resourceId` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`permission` TEXT NOT NULL, " +
                "`permissionId` TEXT NOT NULL, " +
                "PRIMARY KEY(`resourceId`, `userId`), " +
                "FOREIGN KEY(`resourceId`) REFERENCES `Resource`(`resourceId`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`userId`) REFERENCES `User`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val CREATE_FOLDERS_AND_USERS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `FolderAndUsersCrossRef` (" +
                "`folderId` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`permission` TEXT NOT NULL, " +
                "`permissionId` TEXT NOT NULL, " +
                "PRIMARY KEY(`userId`, `folderId`), " +
                "FOREIGN KEY(`folderId`) REFERENCES `Folder`(`folderId`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`userId`) REFERENCES `User`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val CREATE_FOLDERS_AND_GROUPS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS `FolderAndGroupsCrossRef` (" +
                "`folderId` TEXT NOT NULL, " +
                "`groupId` TEXT NOT NULL, " +
                "`permission` TEXT NOT NULL, " +
                "`permissionId` TEXT NOT NULL, " +
                "PRIMARY KEY(`folderId`, `groupId`), " +
                "FOREIGN KEY(`folderId`) REFERENCES `Folder`(`folderId`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`groupId`) REFERENCES `UsersGroup`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            TABLES_TO_DROP.forEach {
                execSQL("DROP TABLE $it")
            }
            execSQL(CREATE_RESOURCE_TABLE)
            execSQL(CREATE_RESOURCE_TYPES_AND_FIELDS_CROSS_REF_TABLE)
            execSQL(CREATE_RESOURCE_AND_TAGS_CROSS_REF_TABLE)
            execSQL(CREATE_RESOURCE_AND_GROUPS_CROSS_REF_TABLE)
            execSQL(CREATE_USERS_AND_GROUPS_CROSS_REF_TABLE)
            execSQL(CREATE_RESOURCE_AND_USERS_CROSS_REF_TABLE)
            execSQL(CREATE_FOLDERS_AND_USERS_CROSS_REF_TABLE)
            execSQL(CREATE_FOLDERS_AND_GROUPS_CROSS_REF_TABLE)
        }
    }
}
