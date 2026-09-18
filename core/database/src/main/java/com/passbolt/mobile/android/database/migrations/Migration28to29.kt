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
object Migration28to29 : Migration(28, 29) {
    private const val CREATE_USER_FTS_TABLE =
        "CREATE VIRTUAL TABLE IF NOT EXISTS `UserFts` USING FTS4(" +
            "`userName`, `firstName`, `lastName`, content=`User`, tokenize=unicode61)"
    private const val CREATE_USER_FTS_BEFORE_UPDATE_TRIGGER =
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_UserFts_BEFORE_UPDATE " +
            "BEFORE UPDATE ON `User` BEGIN " +
            "DELETE FROM `UserFts` WHERE docid=OLD.rowid; END"
    private const val CREATE_USER_FTS_BEFORE_DELETE_TRIGGER =
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_UserFts_BEFORE_DELETE " +
            "BEFORE DELETE ON `User` BEGIN " +
            "DELETE FROM `UserFts` WHERE docid=OLD.rowid; END"
    private const val CREATE_USER_FTS_AFTER_INSERT_TRIGGER =
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_UserFts_AFTER_INSERT " +
            "AFTER INSERT ON `User` BEGIN " +
            "INSERT INTO `UserFts`(docid, `userName`, `firstName`, `lastName`) " +
            "VALUES (NEW.rowid, NEW.`userName`, NEW.`firstName`, NEW.`lastName`); END"
    private const val CREATE_USER_FTS_AFTER_UPDATE_TRIGGER =
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_UserFts_AFTER_UPDATE " +
            "AFTER UPDATE ON `User` BEGIN " +
            "INSERT INTO `UserFts`(docid, `userName`, `firstName`, `lastName`) " +
            "VALUES (NEW.rowid, NEW.`userName`, NEW.`firstName`, NEW.`lastName`); END"
    private const val POPULATE_USER_FTS_FROM_USERS =
        "INSERT INTO `UserFts`(`UserFts`) VALUES('rebuild')"

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_USER_FTS_TABLE)
        connection.execSQL(CREATE_USER_FTS_BEFORE_UPDATE_TRIGGER)
        connection.execSQL(CREATE_USER_FTS_BEFORE_DELETE_TRIGGER)
        connection.execSQL(CREATE_USER_FTS_AFTER_INSERT_TRIGGER)
        connection.execSQL(CREATE_USER_FTS_AFTER_UPDATE_TRIGGER)
        connection.execSQL(POPULATE_USER_FTS_FROM_USERS)
    }
}
