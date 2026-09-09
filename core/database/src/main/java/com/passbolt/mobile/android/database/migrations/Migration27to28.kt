package com.passbolt.mobile.android.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Offline mode: the per-resource "available offline" marks and the encrypted
 * secret cache. Both cascade-delete with their resource.
 */
@Suppress("MagicNumber")
object Migration27to28 : Migration(27, 28) {
    private const val CREATE_OFFLINE_ITEM =
        "CREATE TABLE IF NOT EXISTS `OfflineItem` (" +
            "`resourceId` TEXT NOT NULL, " +
            "`markedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`resourceId`), " +
            "FOREIGN KEY(`resourceId`) REFERENCES `Resource`(`resourceId`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    private const val CREATE_OFFLINE_SECRET =
        "CREATE TABLE IF NOT EXISTS `OfflineSecret` (" +
            "`resourceId` TEXT NOT NULL, " +
            "`encryptedSecret` TEXT NOT NULL, " +
            "`resourceModified` INTEGER NOT NULL, " +
            "`cachedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`resourceId`), " +
            "FOREIGN KEY(`resourceId`) REFERENCES `Resource`(`resourceId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_OFFLINE_ITEM)
        db.execSQL(CREATE_OFFLINE_SECRET)
    }
}
