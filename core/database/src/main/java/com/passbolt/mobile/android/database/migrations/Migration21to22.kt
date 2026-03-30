package com.passbolt.mobile.android.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
object Migration21to22 : Migration(21, 22) {
    private enum class FtsTable(
        val ftsTableName: String,
        val contentTableName: String,
        val columns: List<String>,
    ) {
        RESOURCE_METADATA(
            ftsTableName = "ResourceMetadataFts",
            contentTableName = "ResourceMetadata",
            columns = listOf("name", "username", "customFieldsKeys"),
        ),
        RESOURCE_URI(
            ftsTableName = "ResourceUriFts",
            contentTableName = "ResourceUri",
            columns = listOf("uri"),
        ),
        TAG(
            ftsTableName = "TagFts",
            contentTableName = "Tag",
            columns = listOf("slug"),
        ),
        FOLDER(
            ftsTableName = "FolderFts",
            contentTableName = "Folder",
            columns = listOf("name"),
        ),
        USERS_GROUP(
            ftsTableName = "UsersGroupFts",
            contentTableName = "UsersGroup",
            columns = listOf("name"),
        ),
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        FtsTable.entries.forEach { table ->
            createFtsTable(db, table)
            createSyncTriggers(db, table)
            populateFromContentTable(db, table)
        }
    }

    private fun createFtsTable(
        db: SupportSQLiteDatabase,
        table: FtsTable,
    ) {
        val columns = table.columns.toQuotedCsv()
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `${table.ftsTableName}` USING FTS4(" +
                "$columns, content=`${table.contentTableName}`, tokenize=unicode61)",
        )
    }

    private fun createSyncTriggers(
        db: SupportSQLiteDatabase,
        table: FtsTable,
    ) {
        val columns = table.columns.toQuotedCsv()
        val newValues = table.columns.joinToString(", ") { "NEW.`$it`" }

        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_${table.ftsTableName}_BEFORE_UPDATE " +
                "BEFORE UPDATE ON `${table.contentTableName}` BEGIN " +
                "DELETE FROM `${table.ftsTableName}` WHERE docid=OLD.rowid; END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_${table.ftsTableName}_BEFORE_DELETE " +
                "BEFORE DELETE ON `${table.contentTableName}` BEGIN " +
                "DELETE FROM `${table.ftsTableName}` WHERE docid=OLD.rowid; END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_${table.ftsTableName}_AFTER_INSERT " +
                "AFTER INSERT ON `${table.contentTableName}` BEGIN " +
                "INSERT INTO `${table.ftsTableName}`(docid, $columns) " +
                "VALUES (NEW.rowid, $newValues); END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_${table.ftsTableName}_AFTER_UPDATE " +
                "AFTER UPDATE ON `${table.contentTableName}` BEGIN " +
                "INSERT INTO `${table.ftsTableName}`(docid, $columns) " +
                "VALUES (NEW.rowid, $newValues); END",
        )
    }

    private fun populateFromContentTable(
        db: SupportSQLiteDatabase,
        table: FtsTable,
    ) {
        db.execSQL(
            "INSERT INTO `${table.ftsTableName}`(`${table.ftsTableName}`) VALUES('rebuild')",
        )
    }

    private fun List<String>.toQuotedCsv() = joinToString(", ") { "`$it`" }
}
