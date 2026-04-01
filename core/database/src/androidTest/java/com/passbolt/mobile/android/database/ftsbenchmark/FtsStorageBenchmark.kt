package com.passbolt.mobile.android.database.ftsbenchmark

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.ftsbenchmark.FtsBenchmarkDataFactory.DataSet
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

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
 *
 * Benchmark comparing disk usage of a database with FTS indexes vs without.
 *
 * Uses file-based Room databases so actual file sizes can be measured.
 * WAL is checkpointed before measuring to ensure all data is flushed to the main file.
 */
class FtsStorageBenchmark {
    private lateinit var context: Context
    private lateinit var ftsDbFile: File
    private lateinit var likeDbFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        ftsDbFile = context.getDatabasePath(FTS_DB_NAME)
        likeDbFile = context.getDatabasePath(LIKE_DB_NAME)
        deleteDbFiles(ftsDbFile)
        deleteDbFiles(likeDbFile)
    }

    @After
    fun tearDown() {
        deleteDbFiles(ftsDbFile)
        deleteDbFiles(likeDbFile)
    }

    @Test
    fun benchmarkDiskUsage() {
        printHeader()
        DATASET_SIZES.forEach { size ->
            val dataSet = FtsBenchmarkDataFactory.createDataSet(size)

            val ftsSize = measurePopulatedDbSize(FTS_DB_NAME, dataSet, dropTriggers = false)
            val likeSize = measurePopulatedDbSize(LIKE_DB_NAME, dataSet, dropTriggers = true)

            printStorageResult(size, ftsSize, likeSize)
        }
    }

    private fun measurePopulatedDbSize(
        dbName: String,
        dataSet: DataSet,
        dropTriggers: Boolean,
    ): Long {
        val db = Room.databaseBuilder(context, ResourceDatabase::class.java, dbName).build()
        try {
            if (dropTriggers) dropFtsSyncTriggers(db)

            runBlocking {
                with(db) {
                    resourceTypesDao().insert(FtsBenchmarkDataFactory.createResourceType())
                    tagsDao().insertAll(dataSet.tags)
                    dataSet.resources.chunked(BATCH_SIZE).forEach { resourcesDao().upsertAll(it) }
                    dataSet.metadata.chunked(BATCH_SIZE).forEach { resourceMetadataDao().upsertAll(it) }
                    dataSet.uris.chunked(BATCH_SIZE).forEach { resourceUriDao().insertAll(it) }
                    dataSet.tagCrossRefs.chunked(BATCH_SIZE).forEach {
                        resourcesAndTagsCrossRefDao().insertAll(it)
                    }
                }
            }

            // Flush WAL to main file so file size reflects all data
            val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)")
            cursor.close()
        } finally {
            db.close()
        }

        val dbFile = context.getDatabasePath(dbName)
        val totalSize = getDatabaseSize(dbFile)
        deleteDbFiles(dbFile)
        return totalSize
    }

    private fun dropFtsSyncTriggers(db: ResourceDatabase) {
        with(db.openHelper.writableDatabase) {
            FTS_TABLE_NAMES.forEach { table ->
                TRIGGER_SUFFIXES.forEach { suffix ->
                    execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_${table}_$suffix")
                }
            }
        }
    }

    private fun getDatabaseSize(dbFile: File): Long {
        var totalSize = dbFile.length()
        File(dbFile.path + "-wal").let { if (it.exists()) totalSize += it.length() }
        File(dbFile.path + "-shm").let { if (it.exists()) totalSize += it.length() }
        return totalSize
    }

    private fun deleteDbFiles(dbFile: File) {
        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
    }

    private fun printHeader() {
        println("\n$SEPARATOR")
        println("BENCHMARK: DISK USAGE (FTS index overhead)")
        println(SEPARATOR)
    }

    private fun printStorageResult(
        datasetSize: Int,
        ftsBytes: Long,
        likeBytes: Long,
    ) {
        val overheadBytes = ftsBytes - likeBytes
        val overheadPercent =
            if (likeBytes > 0) {
                overheadBytes * 100 / likeBytes
            } else {
                0L
            }
        println(
            "  %6d resources | FTS: %8d KB | No FTS: %8d KB | overhead: %6d KB (+%d%%)".format(
                datasetSize,
                ftsBytes / BYTES_IN_KB,
                likeBytes / BYTES_IN_KB,
                overheadBytes / BYTES_IN_KB,
                overheadPercent,
            ),
        )
    }

    private companion object {
        val DATASET_SIZES = listOf(1_000, 5_000, 10_000)
        const val BATCH_SIZE = 2_000
        const val BYTES_IN_KB = 1024
        const val FTS_DB_NAME = "benchmark_fts.db"
        const val LIKE_DB_NAME = "benchmark_no_fts.db"
        const val SEPARATOR = "======================================================================"

        val FTS_TABLE_NAMES =
            listOf(
                "ResourceMetadataFts",
                "ResourceUriFts",
                "TagFts",
                "FolderFts",
                "UsersGroupFts",
            )

        val TRIGGER_SUFFIXES =
            listOf(
                "BEFORE_UPDATE",
                "BEFORE_DELETE",
                "AFTER_INSERT",
                "AFTER_UPDATE",
            )
    }
}
