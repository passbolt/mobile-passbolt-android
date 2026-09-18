package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteStatement
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

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
class IndexQueryPlanTest {
    private lateinit var db: ResourceDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, ResourceDatabase::class.java)
                .setDriver(testSqlCipherDriver())
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun homePaginatedQueryStreamsFromModifiedIndexWithoutSorting() =
        runTest {
            val plan =
                queryPlan(
                    paginated(HOME_BY_MODIFIED_SQL),
                    arrayOf<Any?>(SLUG, null, null, null, null, PAGE_SIZE, OFFSET_FIRST_PAGE),
                )

            assertThat(plan).contains("index_Resource_modified_resourceId")
            assertThat(plan).doesNotContain(TEMP_B_TREE_FOR_ORDER_BY)
        }

    @Test
    fun allFoldersPaginatedQueryUsesModifiedIndexForOrderingAndParentIdIndexForCounts() =
        runTest {
            val plan =
                queryPlan(
                    paginated(ALL_FOLDERS_SQL),
                    arrayOf(null, null, PAGE_SIZE, OFFSET_FIRST_PAGE),
                )

            assertThat(plan).contains("index_Folder_modified_folderId")
            assertThat(plan).contains("index_Folder_parentId")
        }

    @Test
    fun recursiveSubFoldersQueryUsesParentIdIndex() =
        runTest {
            val plan =
                queryPlan(
                    RECURSIVE_SUB_FOLDERS_SQL,
                    arrayOf(FOLDER_ID, null, null),
                )

            assertThat(plan).contains("index_Folder_parentId")
        }

    @Test
    fun expiredResourcesPaginatedQueryUsesExpiryIndexWithoutSorting() =
        runTest {
            val plan =
                queryPlan(
                    paginated(EXPIRED_RESOURCES_SQL),
                    arrayOf<Any?>(EXPIRY_MILLIS, SLUG, null, null, null, null, PAGE_SIZE, OFFSET_FIRST_PAGE),
                )

            assertThat(plan).contains("index_Resource_expiry_resourceId")
            assertThat(plan).doesNotContain(TEMP_B_TREE_FOR_ORDER_BY)
        }

    @Test
    fun folderScopedResourcesQueryKeepsDrivingFromFolderIdIndex() =
        runTest {
            val plan =
                queryPlan(
                    paginated(FOLDER_RESOURCES_SQL),
                    arrayOf<Any?>(FOLDER_ID, SLUG, null, null, null, null, PAGE_SIZE, OFFSET_FIRST_PAGE),
                )

            assertThat(plan).contains("index_Resource_folderId")
        }

    @Test
    fun groupScopedResourcesQueryKeepsDrivingFromCrossRefIndex() =
        runTest {
            val plan =
                queryPlan(
                    paginated(GROUP_RESOURCES_SQL),
                    arrayOf<Any?>(GROUP_ID, SLUG, null, null, null, PAGE_SIZE, OFFSET_FIRST_PAGE),
                )

            assertThat(plan).contains("index_ResourceAndGroupsCrossRef_groupId")
        }

    private suspend fun queryPlan(
        sql: String,
        args: Array<out Any?>,
    ): String =
        db.useReaderConnection { transactor ->
            transactor.usePrepared("EXPLAIN QUERY PLAN $sql") { statement ->
                args.forEachIndexed { index, arg -> statement.bind(index + 1, arg) }
                buildString {
                    while (statement.step()) {
                        appendLine(statement.getText(statement.getColumnNames().indexOf("detail")))
                    }
                }
            }
        }

    private fun SQLiteStatement.bind(
        index: Int,
        value: Any?,
    ) {
        when (value) {
            null -> bindNull(index)
            is String -> bindText(index, value)
            is Int -> bindInt(index, value)
            is Long -> bindLong(index, value)
            else -> error("Unsupported bind type: ${value::class}")
        }
    }

    private fun paginated(sql: String) = "SELECT * FROM ($sql) LIMIT ? OFFSET ?"

    private companion object {
        const val SLUG = "password-and-description"
        const val FOLDER_ID = "folder-id"
        const val GROUP_ID = "group-id"
        const val EXPIRY_MILLIS = 1644909225833L
        const val PAGE_SIZE = 50
        const val OFFSET_FIRST_PAGE = 0
        const val TEMP_B_TREE_FOR_ORDER_BY = "USE TEMP B-TREE FOR ORDER BY"

        const val HOME_BY_MODIFIED_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, rt.slug, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm " +
                "ON r.resourceId = rm.resourceId " +
                "INNER JOIN ResourceType rt " +
                "ON r.resourceTypeId = rt.resourceTypeId " +
                "WHERE rt.slug IN (?) " +
                "AND (" +
                "   ? IS NULL OR (" +
                "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH ? AND docid = rm.rowid) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
                "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH ? " +
                "       AND ResourceUri.resourceId = r.resourceId" +
                "   ) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
                "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH ? " +
                "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
                "   )" +
                ")) " +
                "ORDER BY r.modified DESC, r.resourceId ASC"

        const val ALL_FOLDERS_SQL =
            "SELECT folderId, name, permission, parentId, isShared, " +
                "(SELECT " +
                "( " +
                "(SELECT count(*) FROM Folder f_count WHERE f_count.parentId is f.folderId)" +
                " + " +
                "(SELECT count(*) FROM Resource r_count WHERE r_count.folderId is f.folderId)" +
                ")" +
                ") AS childItemsCount, " +
                "(" +
                "WITH RECURSIVE ancestor(folderId, name, parentId, level) as ( " +
                "   SELECT folderId, name, parentId, 0  " +
                "   FROM Folder  " +
                "   WHERE folderId = f.folderId" +
                "" +
                "   UNION ALL  " +
                "" +
                "   SELECT f.folderId, f.name, f.parentId, a.level - 1  " +
                "   FROM Folder f  " +
                "   JOIN ancestor a on f.folderId = a.parentId  " +
                ") " +
                "SELECT GROUP_CONCAT(name, ' › ')" +
                "FROM " +
                "   (SELECT name FROM ancestor a order by a.level)" +
                ") as path " +
                "FROM Folder f " +
                "WHERE ? IS NULL OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM FolderFts WHERE FolderFts MATCH ? AND FolderFts.docid = f.rowid" +
                "   ) " +
                "ORDER BY f.modified DESC, f.folderId ASC"

        const val RECURSIVE_SUB_FOLDERS_SQL =
            "WITH RECURSIVE ancestor(folderId, name, permission, parentId, isShared, level) as (" +
                "SELECT folderId, name, permission, parentId, isShared, 0 " +
                "from Folder " +
                "WHERE folderId IS ? " +
                "" +
                "UNION ALL " +
                "" +
                "SELECT f.folderId, f.name, f.permission, f.parentId, f.isShared, a.level + 1 " +
                "FROM Folder f " +
                "JOIN ancestor a on f.parentId = a.folderId " +
                ") " +
                "" +
                "SELECT folderId, name, permission, parentId, isShared, " +
                "(SELECT" +
                "(" +
                "(select count(*) from  folder fc where fc.parentId is a.folderId) + " +
                "(select count(*) from resource rc where rc.folderId is a.folderId) " +
                ")" +
                ") AS childItemsCount, " +
                "(" +
                "WITH RECURSIVE ancestor_path(folderId, name, parentId, level) as ( " +
                "   SELECT folderId, name, parentId, 0  " +
                "   FROM Folder  " +
                "   WHERE folderId = a.folderId" +
                "" +
                "   UNION ALL  " +
                "" +
                "   SELECT f.folderId, f.name, f.parentId, ap.level - 1  " +
                "   FROM Folder f  " +
                "   JOIN ancestor_path ap on f.folderId = ap.parentId  " +
                ") " +
                "SELECT GROUP_CONCAT(name, ' › ')" +
                "FROM " +
                "   (SELECT name FROM ancestor_path order by ancestor_path.level)" +
                ") as path " +
                "" +
                "FROM ancestor a " +
                "WHERE level > 0 " +
                "AND (? IS NULL OR " +
                "   a.folderId IN (" +
                "       SELECT Folder.folderId FROM FolderFts, Folder " +
                "       WHERE FolderFts.docid = Folder.rowid AND FolderFts MATCH ?" +
                "   )" +
                ") " +
                "ORDER BY level"

        const val EXPIRED_RESOURCES_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, rt.slug, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm " +
                "ON r.resourceId = rm.resourceId " +
                "INNER JOIN ResourceType rt " +
                "ON r.resourceTypeId = rt.resourceTypeId " +
                "WHERE r.expiry IS NOT NULL AND r.expiry < ? AND rt.slug IN (?) " +
                "AND (" +
                "   ? IS NULL OR (" +
                "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH ? AND docid = rm.rowid) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
                "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH ? " +
                "       AND ResourceUri.resourceId = r.resourceId" +
                "   ) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
                "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH ? " +
                "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
                "   )" +
                ")) " +
                "ORDER BY expiry ASC, r.resourceId ASC"

        const val FOLDER_RESOURCES_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, rt.slug, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm " +
                "ON r.resourceId = rm.resourceId " +
                "INNER JOIN ResourceType rt " +
                "ON r.resourceTypeId = rt.resourceTypeId " +
                "WHERE r.folderId IS ? AND r.resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (?)" +
                ") " +
                "AND ( " +
                "   ? IS NULL OR (" +
                "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH ? AND docid = rm.rowid) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
                "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH ? " +
                "       AND ResourceUri.resourceId = r.resourceId" +
                "   ) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
                "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH ? " +
                "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
                "   )" +
                ")) " +
                "ORDER BY rm.name COLLATE NOCASE ASC, r.resourceId ASC"

        const val GROUP_RESOURCES_SQL =
            "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
                "r.resourcePermission, r.resourceTypeId, rt.slug, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
                "FROM Resource r " +
                "INNER JOIN ResourceMetadata rm " +
                "ON r.resourceId = rm.resourceId " +
                "INNER JOIN ResourceType rt " +
                "ON r.resourceTypeId = rt.resourceTypeId " +
                "INNER JOIN ResourceAndGroupsCrossRef cr " +
                "ON r.resourceId=cr.resourceId " +
                "WHERE cr.groupId=? AND r.resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (?)" +
                ") " +
                "AND (" +
                "   ? IS NULL OR (" +
                "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH ? AND docid = rm.rowid) OR " +
                "   EXISTS (" +
                "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
                "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH ? " +
                "       AND ResourceUri.resourceId = r.resourceId" +
                "   )" +
                ")) " +
                "ORDER BY rm.name COLLATE NOCASE ASC, r.resourceId ASC"
    }
}
