package com.passbolt.mobile.android.database.ftsbenchmark

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.ftsbenchmark.FtsBenchmarkDataFactory.DataSet
import com.passbolt.mobile.android.database.ftsbenchmark.FtsBenchmarkDataFactory.SLUG
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

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
 * Benchmark comparing FTS4 MATCH search vs LIKE substring search performance.
 *
 * Uses two in-memory Room databases:
 * - [ftsDb]: FTS sync triggers intact (production behavior on FTS branch)
 * - [likeDb]: FTS sync triggers dropped (simulates pre-FTS behavior on develop)
 *
 * This isolates the database layer from encryption, network, and UI overhead.
 * Run on a device/emulator and check Logcat for results.
 */
class FtsVsLikeBenchmark {
    private lateinit var ftsDb: ResourceDatabase
    private lateinit var likeDb: ResourceDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ftsDb = Room.inMemoryDatabaseBuilder(context, ResourceDatabase::class.java).build()
        likeDb = Room.inMemoryDatabaseBuilder(context, ResourceDatabase::class.java).build()
        dropFtsSyncTriggers(likeDb)
    }

    @After
    fun tearDown() {
        ftsDb.close()
        likeDb.close()
    }

    @Test
    fun benchmarkInsert() {
        printHeader("INSERT")
        DATASET_SIZES.forEach { size ->
            val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
            runBlocking {
                seedReferenceData(ftsDb, dataSet)
                seedReferenceData(likeDb, dataSet)
            }

            val ftsDuration =
                measureTime {
                    runBlocking { insertResourceData(ftsDb, dataSet) }
                }
            val likeDuration =
                measureTime {
                    runBlocking { insertResourceData(likeDb, dataSet) }
                }

            printResult("INSERT", size, ftsDuration, likeDuration)
            clearBothDatabases()
        }
    }

    @Test
    fun benchmarkSearch() {
        printHeader("SEARCH")
        DATASET_SIZES.forEach { size ->
            val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
            runBlocking { populateBothDatabases(dataSet) }

            // Warm up SQLite page cache
            searchWithFts(ftsDb)
            searchWithLike(likeDb)

            val (ftsCount, ftsDuration) = runSearchIterations { searchWithFts(ftsDb) }
            val (likeCount, likeDuration) = runSearchIterations { searchWithLike(likeDb) }

            assertThat(ftsCount).isEqualTo(likeCount)
            printResult(
                "SEARCH ($SEARCH_ITERATIONS iterations)",
                size,
                ftsDuration,
                likeDuration,
                ftsCount,
            )
            clearBothDatabases()
        }
    }

    @Test
    fun benchmarkUpdate() {
        printHeader("UPDATE")
        DATASET_SIZES.forEach { size ->
            val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
            runBlocking { populateBothDatabases(dataSet) }

            val updatedMetadata = dataSet.metadata.map { it.copy(name = "Updated ${it.name}") }

            val ftsDuration =
                measureTime {
                    runBlocking {
                        updatedMetadata.chunked(BATCH_SIZE).forEach {
                            ftsDb.resourceMetadataDao().upsertAll(it)
                        }
                    }
                }
            val likeDuration =
                measureTime {
                    runBlocking {
                        updatedMetadata.chunked(BATCH_SIZE).forEach {
                            likeDb.resourceMetadataDao().upsertAll(it)
                        }
                    }
                }

            printResult("UPDATE", size, ftsDuration, likeDuration)
            clearBothDatabases()
        }
    }

    @Test
    fun benchmarkFetchAll() {
        printHeader("FETCH ALL (no search query - control)")
        DATASET_SIZES.forEach { size ->
            val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
            runBlocking { populateBothDatabases(dataSet) }

            // Warm up
            fetchAll(ftsDb)
            fetchAll(likeDb)

            val (ftsCount, ftsDuration) = runSearchIterations { fetchAll(ftsDb) }
            val (likeCount, likeDuration) = runSearchIterations { fetchAll(likeDb) }

            assertThat(ftsCount).isEqualTo(likeCount)
            assertThat(ftsCount).isEqualTo(size)
            printResult(
                "FETCH ALL ($SEARCH_ITERATIONS iterations)",
                size,
                ftsDuration,
                likeDuration,
                ftsCount,
            )
            clearBothDatabases()
        }
    }

    @Test
    fun benchmarkDelete() {
        printHeader("DELETE (cascade from Resource)")
        DATASET_SIZES.forEach { size ->
            val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
            runBlocking { populateBothDatabases(dataSet) }

            val ftsDuration =
                measureTime {
                    ftsDb.openHelper.writableDatabase.execSQL("DELETE FROM Resource")
                }
            val likeDuration =
                measureTime {
                    likeDb.openHelper.writableDatabase.execSQL("DELETE FROM Resource")
                }

            printResult("DELETE", size, ftsDuration, likeDuration)
            clearBothDatabases()
        }
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

    private fun clearBothDatabases() {
        ftsDb.clearAllTables()
        likeDb.clearAllTables()
    }

    private suspend fun seedReferenceData(
        db: ResourceDatabase,
        dataSet: DataSet,
    ) {
        with(db) {
            resourceTypesDao().insert(FtsBenchmarkDataFactory.createResourceType())
            tagsDao().insertAll(dataSet.tags)
        }
    }

    private suspend fun insertResourceData(
        db: ResourceDatabase,
        dataSet: DataSet,
    ) {
        with(dataSet) {
            resources.chunked(BATCH_SIZE).forEach { db.resourcesDao().upsertAll(it) }
            metadata.chunked(BATCH_SIZE).forEach { db.resourceMetadataDao().upsertAll(it) }
            uris.chunked(BATCH_SIZE).forEach { db.resourceUriDao().insertAll(it) }
            tagCrossRefs.chunked(BATCH_SIZE).forEach {
                db.resourcesAndTagsCrossRefDao().insertAll(it)
            }
        }
    }

    private suspend fun populateBothDatabases(dataSet: DataSet) {
        seedReferenceData(ftsDb, dataSet)
        seedReferenceData(likeDb, dataSet)
        insertResourceData(ftsDb, dataSet)
        insertResourceData(likeDb, dataSet)
    }

    private fun searchWithFts(db: ResourceDatabase): Int {
        val ftsQuery = "$SEARCH_TERM*"
        val cursor =
            db.query(
                SimpleSQLiteQuery(FTS_SEARCH_SQL, arrayOf(SLUG, ftsQuery, ftsQuery, ftsQuery)),
            )
        val count = cursor.count
        cursor.close()
        return count
    }

    private fun searchWithLike(db: ResourceDatabase): Int {
        val cursor =
            db.query(
                SimpleSQLiteQuery(
                    LIKE_SEARCH_SQL,
                    arrayOf(SLUG, SEARCH_TERM, SEARCH_TERM, SEARCH_TERM, SEARCH_TERM, SEARCH_TERM),
                ),
            )
        val count = cursor.count
        cursor.close()
        return count
    }

    private fun fetchAll(db: ResourceDatabase): Int {
        val cursor = db.query(SimpleSQLiteQuery(FETCH_ALL_SQL, arrayOf(SLUG)))
        val count = cursor.count
        cursor.close()
        return count
    }

    private fun runSearchIterations(query: () -> Int): Pair<Int, Duration> =
        measureTimedValue {
            var count = 0
            repeat(SEARCH_ITERATIONS) { count = query() }
            count
        }.let { it.value to it.duration }

    private fun printHeader(operation: String) {
        println("\n$SEPARATOR")
        println("BENCHMARK: $operation")
        println(SEPARATOR)
    }

    @Suppress("LongParameterList")
    private fun printResult(
        operation: String,
        datasetSize: Int,
        ftsDuration: Duration,
        likeDuration: Duration,
        resultCount: Int? = null,
    ) {
        val diffPercent = calculateDiffPercent(ftsDuration, likeDuration)
        val resultsSuffix = resultCount?.let { " | results: $it" }.orEmpty()
        println(
            "  %-35s | %6d resources | FTS: %6dms | LIKE: %6dms | diff: %s%s".format(
                operation,
                datasetSize,
                ftsDuration.inWholeMilliseconds,
                likeDuration.inWholeMilliseconds,
                diffPercent,
                resultsSuffix,
            ),
        )
    }

    private fun calculateDiffPercent(
        ftsDuration: Duration,
        likeDuration: Duration,
    ): String {
        if (likeDuration.inWholeMilliseconds == 0L) return "N/A"
        val differencePercent =
            (ftsDuration.inWholeMilliseconds - likeDuration.inWholeMilliseconds) * 100 /
                likeDuration.inWholeMilliseconds
        return if (differencePercent >= 0) "+$differencePercent%" else "$differencePercent%"
    }

    private companion object {
        val DATASET_SIZES = listOf(1_000, 5_000, 10_000)
        const val SEARCH_TERM = "Gmail"
        const val SEARCH_ITERATIONS = 10
        const val BATCH_SIZE = 2_000
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

        const val FTS_SEARCH_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm ON r.resourceId = rm.resourceId " +
                "WHERE r.resourceTypeId IN (SELECT resourceTypeId FROM ResourceType WHERE slug = ?) " +
                "AND (" +
                "EXISTS (SELECT 1 FROM ResourceMetadataFts " +
                "WHERE ResourceMetadataFts MATCH ? AND docid = rm.rowid) " +
                "OR EXISTS (" +
                "SELECT 1 FROM ResourceUriFts, ResourceUri " +
                "WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH ? " +
                "AND ResourceUri.resourceId = r.resourceId" +
                ") " +
                "OR EXISTS (" +
                "SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
                "WHERE TagFts.docid = Tag.rowid AND TagFts MATCH ? " +
                "AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
                ")" +
                ") " +
                "ORDER BY rm.name COLLATE NOCASE ASC"

        const val LIKE_SEARCH_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm ON r.resourceId = rm.resourceId " +
                "WHERE r.resourceTypeId IN (SELECT resourceTypeId FROM ResourceType WHERE slug = ?) " +
                "AND (" +
                "rm.name LIKE '%' || ? || '%' " +
                "OR rm.username LIKE '%' || ? || '%' " +
                "OR rm.customFieldsKeys LIKE '%' || ? || '%' " +
                "OR EXISTS (" +
                "SELECT 1 FROM ResourceUri ru " +
                "WHERE ru.resourceId = r.resourceId AND ru.uri LIKE '%' || ? || '%'" +
                ") " +
                "OR EXISTS (" +
                "SELECT 1 FROM ResourceAndTagsCrossRef rTCR " +
                "INNER JOIN Tag t ON t.id = rTCR.tagId " +
                "WHERE rTCR.resourceId = r.resourceId AND t.slug LIKE '%' || ? || '%'" +
                ")" +
                ") " +
                "ORDER BY rm.name COLLATE NOCASE ASC"

        const val FETCH_ALL_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm ON r.resourceId = rm.resourceId " +
                "WHERE r.resourceTypeId IN (SELECT resourceTypeId FROM ResourceType WHERE slug = ?) " +
                "ORDER BY rm.name COLLATE NOCASE ASC"
    }
}
