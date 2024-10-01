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
object Migration7to8 : Migration(7, 8) {

    private const val CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS User (" +
            "`id` TEXT NOT NULL, " +
            "`userName` TEXT NOT NULL, " +
            "`firstName` TEXT, " +
            "`lastName` TEXT, " +
            "`avatarUrl` TEXT, " +
            "`armoredKey` TEXT NOT NULL, " +
            "`bits` INTEGER NOT NULL, " +
            "`uid` TEXT, " +
            "`keyId` TEXT NOT NULL, " +
            "`fingerprint` TEXT NOT NULL, " +
            "`type` TEXT, " +
            "`expires` INTEGER, " +
            "PRIMARY KEY(`id`))"

    private const val DROP_RESOURCE_AND_GROUPS_CROSS_REF_TABLE = "DROP TABLE ResourceAndGroupsCrossRef"
    private const val CREATE_RESOURCE_AND_GROUPS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS ResourceAndGroupsCrossRef (" +
                "`resourceId` TEXT NOT NULL, " +
                "`groupId` TEXT NOT NULL, " +
                "`permission` TEXT NOT NULL, " +
                "PRIMARY KEY(`resourceId`, `groupId`))"

    private const val CREATE_USERS_AND_GROUPS_CROSS_REF_TABLE = "CREATE TABLE IF NOT EXISTS UsersAndGroupCrossRef (" +
            "`userId` TEXT NOT NULL, " +
            "`groupId` TEXT NOT NULL, " +
            "PRIMARY KEY(`userId`, `groupId`))"

    private const val CREATE_RESOURCE_AND_USERS_CROSS_REF_TABLE =
        "CREATE TABLE IF NOT EXISTS ResourceAndUsersCrossRef (" +
                "`resourceId` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`permission` TEXT NOT NULL, " +
                "PRIMARY KEY(`resourceId`, `userId`))"

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            execSQL(CREATE_USERS_TABLE)
            execSQL(DROP_RESOURCE_AND_GROUPS_CROSS_REF_TABLE)
            execSQL(CREATE_RESOURCE_AND_GROUPS_CROSS_REF_TABLE)
            execSQL(CREATE_USERS_AND_GROUPS_CROSS_REF_TABLE)
            execSQL(CREATE_RESOURCE_AND_USERS_CROSS_REF_TABLE)
        }
    }
}
