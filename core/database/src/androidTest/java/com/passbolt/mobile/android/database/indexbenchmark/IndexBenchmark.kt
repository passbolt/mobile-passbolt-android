package com.passbolt.mobile.android.database.indexbenchmark

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.ftsbenchmark.FtsBenchmarkDataFactory
import com.passbolt.mobile.android.database.ftsbenchmark.FtsBenchmarkDataFactory.DataSet
import com.passbolt.mobile.android.database.ftsbenchmark.FtsBenchmarkDataFactory.SLUG
import com.passbolt.mobile.android.database.helpers.executeSql
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState.PENDING
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.measureTime

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
class IndexBenchmark {
    private lateinit var indexedDb: ResourceDatabase
    private lateinit var baselineDb: ResourceDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        indexedDb = Room.inMemoryDatabaseBuilder(context, ResourceDatabase::class.java).setDriver(testSqlCipherDriver()).build()
        baselineDb = Room.inMemoryDatabaseBuilder(context, ResourceDatabase::class.java).setDriver(testSqlCipherDriver()).build()
        runBlocking { dropNewIndexes(baselineDb) }
    }

    @After
    fun tearDown() {
        indexedDb.close()
        baselineDb.close()
    }

    @Test
    fun benchmarkHomePageLoad() =
        runBlocking {
            printHeader("HOME PAGE LOAD (modified DESC, first + middle page, $ITERATIONS iterations)")
            RESOURCE_DATASET_SIZES.forEach { size ->
                val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
                populateResources(indexedDb, dataSet)
                populateResources(baselineDb, dataSet)

                loadHomePages(indexedDb, size)
                loadHomePages(baselineDb, size)

                val indexedDuration = measureTime { repeat(ITERATIONS) { loadHomePages(indexedDb, size) } }
                val baselineDuration = measureTime { repeat(ITERATIONS) { loadHomePages(baselineDb, size) } }

                printResult("HOME PAGE LOAD", size, indexedDuration, baselineDuration)
                clearBothDatabases()
            }
        }

    @Test
    fun benchmarkAllFoldersPageLoad() =
        runBlocking {
            printHeader("ALL FOLDERS PAGE LOAD (first + middle page, $ITERATIONS iterations)")
            FOLDER_DATASET_SIZES.forEach { folderCount ->
                populateFolders(indexedDb, folderCount)
                populateFolders(baselineDb, folderCount)

                loadFolderPages(indexedDb, folderCount)
                loadFolderPages(baselineDb, folderCount)

                val indexedDuration = measureTime { repeat(ITERATIONS) { loadFolderPages(indexedDb, folderCount) } }
                val baselineDuration = measureTime { repeat(ITERATIONS) { loadFolderPages(baselineDb, folderCount) } }

                printResult("ALL FOLDERS PAGE LOAD", folderCount, indexedDuration, baselineDuration)
                clearBothDatabases()
            }
        }

    @Test
    fun benchmarkRefreshLoop() =
        runBlocking {
            printHeader("REFRESH (mark-and-sweep second sync: mark PENDING, upsert all, sweep)")
            RESOURCE_DATASET_SIZES.forEach { size ->
                val dataSet = FtsBenchmarkDataFactory.createDataSet(size)
                populateResources(indexedDb, dataSet)
                populateResources(baselineDb, dataSet)

                val indexedDuration = measureTime { runRefreshLoop(indexedDb, dataSet) }
                val baselineDuration = measureTime { runRefreshLoop(baselineDb, dataSet) }

                printResult("REFRESH", size, indexedDuration, baselineDuration)
                clearBothDatabases()
            }
        }

    private suspend fun loadHomePages(
        db: ResourceDatabase,
        size: Int,
    ) {
        loadPage(db.paginatedResourcesDao().getAllOrderedByModifiedDatePaginated(setOf(SLUG), null), key = null)
        loadPage(db.paginatedResourcesDao().getAllOrderedByModifiedDatePaginated(setOf(SLUG), null), key = size / 2)
    }

    private suspend fun loadFolderPages(
        db: ResourceDatabase,
        folderCount: Int,
    ) {
        loadPage(db.paginatedFoldersDao().getAllFolders(null), key = null)
        loadPage(db.paginatedFoldersDao().getAllFolders(null), key = folderCount / 2)
    }

    private suspend fun <T : Any> loadPage(
        pagingSource: PagingSource<Int, T>,
        key: Int?,
    ) {
        pagingSource.load(
            PagingSource.LoadParams.Refresh(key = key, loadSize = PAGE_SIZE, placeholdersEnabled = false),
        )
    }

    private suspend fun runRefreshLoop(
        db: ResourceDatabase,
        dataSet: DataSet,
    ) {
        db.resourcesDao().setAllUpdateState(PENDING)
        dataSet.resources.chunked(BATCH_SIZE).forEach { db.resourcesDao().upsertAll(it) }
        dataSet.metadata.chunked(BATCH_SIZE).forEach { db.resourceMetadataDao().upsertAll(it) }
        db.resourcesDao().removeWithUpdateState(PENDING)
    }

    private suspend fun populateResources(
        db: ResourceDatabase,
        dataSet: DataSet,
    ) {
        db.resourceTypesDao().insert(FtsBenchmarkDataFactory.createResourceType())
        db.tagsDao().insertAll(dataSet.tags)
        dataSet.resources.chunked(BATCH_SIZE).forEach { db.resourcesDao().upsertAll(it) }
        dataSet.metadata.chunked(BATCH_SIZE).forEach { db.resourceMetadataDao().upsertAll(it) }
        dataSet.uris.chunked(BATCH_SIZE).forEach { db.resourceUriDao().insertAll(it) }
        dataSet.tagCrossRefs.chunked(BATCH_SIZE).forEach { db.resourcesAndTagsCrossRefDao().insertAll(it) }
    }

    private suspend fun populateFolders(
        db: ResourceDatabase,
        folderCount: Int,
    ) {
        val folders = IndexBenchmarkDataFactory.createFolders(folderCount)
        val resources =
            IndexBenchmarkDataFactory.assignToFolders(
                FtsBenchmarkDataFactory.createDataSet(folderCount).resources,
                folders,
            )
        db.resourceTypesDao().insert(FtsBenchmarkDataFactory.createResourceType())
        folders.chunked(BATCH_SIZE).forEach { db.foldersDao().upsertAll(it) }
        resources.chunked(BATCH_SIZE).forEach { db.resourcesDao().upsertAll(it) }
    }

    private suspend fun dropNewIndexes(db: ResourceDatabase) {
        NEW_INDEX_NAMES.forEach { db.executeSql("DROP INDEX IF EXISTS $it") }
    }

    private suspend fun clearBothDatabases() {
        indexedDb.clearAllTables()
        baselineDb.clearAllTables()
    }

    private fun printHeader(operation: String) {
        println("\n$SEPARATOR")
        println("BENCHMARK: $operation")
        println(SEPARATOR)
    }

    private fun printResult(
        operation: String,
        datasetSize: Int,
        indexedDuration: Duration,
        baselineDuration: Duration,
    ) {
        println(
            "  %-24s | %6d items | INDEXED: %6dms | NO-INDEX: %6dms | diff: %s".format(
                operation,
                datasetSize,
                indexedDuration.inWholeMilliseconds,
                baselineDuration.inWholeMilliseconds,
                calculateDiffPercent(indexedDuration, baselineDuration),
            ),
        )
    }

    private fun calculateDiffPercent(
        indexedDuration: Duration,
        baselineDuration: Duration,
    ): String {
        if (baselineDuration.inWholeMilliseconds == 0L) return "N/A"
        val differencePercent =
            (indexedDuration.inWholeMilliseconds - baselineDuration.inWholeMilliseconds) * 100 /
                baselineDuration.inWholeMilliseconds
        return if (differencePercent >= 0) "+$differencePercent%" else "$differencePercent%"
    }

    private companion object {
        val RESOURCE_DATASET_SIZES = listOf(1_000, 5_000, 10_000)
        val FOLDER_DATASET_SIZES = listOf(200, 1_000, 3_000)
        const val ITERATIONS = 10
        const val PAGE_SIZE = 50
        const val BATCH_SIZE = 2_000
        const val SEPARATOR = "======================================================================"

        val NEW_INDEX_NAMES =
            listOf(
                "index_Folder_parentId",
                "index_Folder_modified_folderId",
                "index_Resource_modified_resourceId",
                "index_Resource_expiry_resourceId",
                "index_Resource_favouriteId",
            )
    }
}
