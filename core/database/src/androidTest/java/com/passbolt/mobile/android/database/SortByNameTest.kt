package com.passbolt.mobile.android.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import com.passbolt.mobile.android.database.impl.folders.PaginatedFoldersDao
import com.passbolt.mobile.android.database.impl.resources.PaginatedResourcesDao
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderUpdateState
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceMetadata
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState
import com.passbolt.mobile.android.entity.resource.ResourceWithMetadata
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

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

class SortByNameTest {
    private lateinit var paginatedResourcesDao: PaginatedResourcesDao
    private lateinit var paginatedFoldersDao: PaginatedFoldersDao
    private lateinit var db: ResourceDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    ResourceDatabase::class.java,
                ).setDriver(testSqlCipherDriver())
                .build()
        paginatedResourcesDao = db.paginatedResourcesDao()
        paginatedFoldersDao = db.paginatedFoldersDao()
        runBlocking {
            db.resourceTypesDao().insert(RESOURCE_TYPE)
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testSortingByNameIsCaseInsensitiveAndBreaksTiesByResourceId() =
        runBlocking {
            db.resourcesDao().insertAll(listOf(RESOURCE_ZEBRA, RESOURCE_BANANA, RESOURCE_APPLE_LOWERCASE, RESOURCE_APPLE_CAPITALIZED))
            db.resourceMetadataDao().insertAll(
                listOf(
                    metadata(RESOURCE_ZEBRA, "Zebra"),
                    metadata(RESOURCE_BANANA, "banana"),
                    metadata(RESOURCE_APPLE_LOWERCASE, "apple"),
                    metadata(RESOURCE_APPLE_CAPITALIZED, "Apple"),
                ),
            )

            val loadResult =
                paginatedResourcesDao
                    .getAllOrderedByNamePaginated(setOf(PASSWORD_DESCRIPTION_SLUG), ftsQuery = null)
                    .load(PagingSource.LoadParams.Refresh(key = null, loadSize = PAGE_SIZE, placeholdersEnabled = false))
            val firstPage = loadResult as PagingSource.LoadResult.Page<Int, ResourceWithMetadata>

            assertThat(firstPage.data.map { it.resourceId })
                .containsExactly(
                    RESOURCE_APPLE_LOWERCASE.resourceId,
                    RESOURCE_APPLE_CAPITALIZED.resourceId,
                    RESOURCE_BANANA.resourceId,
                    RESOURCE_ZEBRA.resourceId,
                ).inOrder()
        }

    @Test
    fun testDirectChildFoldersSortingByNameIsCaseInsensitiveAndBreaksTiesByFolderId() =
        runBlocking {
            db.foldersDao().insertAll(
                listOf(ROOT_FOLDER_ZEBRA, ROOT_FOLDER_BANANA, ROOT_FOLDER_APPLE_LOWERCASE, ROOT_FOLDER_APPLE_CAPITALIZED),
            )

            val firstPage = paginatedFoldersDao.getFolderDirectChildFolders(folderId = null, ftsQuery = null).firstPage()

            assertThat(firstPage.map { it.folderId })
                .containsExactly(
                    ROOT_FOLDER_APPLE_LOWERCASE.folderId,
                    ROOT_FOLDER_APPLE_CAPITALIZED.folderId,
                    ROOT_FOLDER_BANANA.folderId,
                    ROOT_FOLDER_ZEBRA.folderId,
                ).inOrder()
        }

    @Test
    fun testAllFoldersSortingByNameIgnoresFolderLevel() =
        runBlocking {
            db.foldersDao().insertAll(NESTED_FOLDERS)

            val firstPage = paginatedFoldersDao.getAllFolders(ftsQuery = null).firstPage()

            assertThat(firstPage.map { it.folderId })
                .containsExactly(
                    FOLDER_AARDVARK.folderId,
                    FOLDER_APPLE.folderId,
                    FOLDER_BANANA.folderId,
                    FOLDER_CHERRY.folderId,
                    FOLDER_ZEBRA.folderId,
                ).inOrder()
        }

    @Test
    fun testSubFoldersSortingIsByLevelThenByName() =
        runBlocking {
            db.foldersDao().insertAll(NESTED_FOLDERS)

            val firstPage =
                paginatedFoldersDao
                    .getFolderAllChildFoldersRecursively(FOLDER_BANANA.folderId, ftsQuery = null)
                    .firstPage()

            assertThat(firstPage.map { it.folderId })
                .containsExactly(
                    FOLDER_APPLE.folderId,
                    FOLDER_ZEBRA.folderId,
                    FOLDER_AARDVARK.folderId,
                ).inOrder()
        }

    @Test
    fun testResourcesInFolderSortingByNameIsCaseInsensitiveAndBreaksTiesByResourceId() =
        runBlocking {
            db.foldersDao().insertAll(listOf(FOLDER_BANANA))
            db.resourcesDao().insertAll(
                listOf(
                    RESOURCE_ZEBRA.copy(folderId = FOLDER_BANANA.folderId),
                    RESOURCE_BANANA.copy(folderId = FOLDER_BANANA.folderId),
                    RESOURCE_APPLE_LOWERCASE.copy(folderId = FOLDER_BANANA.folderId),
                    RESOURCE_APPLE_CAPITALIZED.copy(folderId = FOLDER_BANANA.folderId),
                ),
            )
            db.resourceMetadataDao().insertAll(
                listOf(
                    metadata(RESOURCE_ZEBRA, "Zebra"),
                    metadata(RESOURCE_BANANA, "banana"),
                    metadata(RESOURCE_APPLE_LOWERCASE, "apple"),
                    metadata(RESOURCE_APPLE_CAPITALIZED, "Apple"),
                ),
            )

            val firstPage =
                paginatedResourcesDao
                    .getResourcesForFolderWithId(FOLDER_BANANA.folderId, setOf(PASSWORD_DESCRIPTION_SLUG), ftsQuery = null)
                    .firstPage()

            assertThat(firstPage.map { it.resourceId })
                .containsExactly(
                    RESOURCE_APPLE_LOWERCASE.resourceId,
                    RESOURCE_APPLE_CAPITALIZED.resourceId,
                    RESOURCE_BANANA.resourceId,
                    RESOURCE_ZEBRA.resourceId,
                ).inOrder()
        }

    @Test
    fun testResourcesInSubFoldersSortingByNameIsCaseInsensitiveAndBreaksTiesByResourceId() =
        runBlocking {
            db.foldersDao().insertAll(listOf(FOLDER_BANANA, FOLDER_CHERRY))
            db.resourcesDao().insertAll(
                listOf(
                    RESOURCE_ZEBRA.copy(folderId = FOLDER_BANANA.folderId),
                    RESOURCE_BANANA.copy(folderId = FOLDER_CHERRY.folderId),
                    RESOURCE_APPLE_LOWERCASE.copy(folderId = FOLDER_BANANA.folderId),
                    RESOURCE_APPLE_CAPITALIZED.copy(folderId = FOLDER_CHERRY.folderId),
                ),
            )
            db.resourceMetadataDao().insertAll(
                listOf(
                    metadata(RESOURCE_ZEBRA, "Zebra"),
                    metadata(RESOURCE_BANANA, "banana"),
                    metadata(RESOURCE_APPLE_LOWERCASE, "apple"),
                    metadata(RESOURCE_APPLE_CAPITALIZED, "Apple"),
                ),
            )

            val firstPage =
                paginatedResourcesDao
                    .getFilteredForChildFolders(
                        listOf(FOLDER_BANANA.folderId, FOLDER_CHERRY.folderId),
                        setOf(PASSWORD_DESCRIPTION_SLUG),
                        ftsQuery = null,
                    ).firstPage()

            assertThat(firstPage.map { it.resourceId })
                .containsExactly(
                    RESOURCE_APPLE_LOWERCASE.resourceId,
                    RESOURCE_APPLE_CAPITALIZED.resourceId,
                    RESOURCE_BANANA.resourceId,
                    RESOURCE_ZEBRA.resourceId,
                ).inOrder()
        }

    private suspend fun <T : Any> PagingSource<Int, T>.firstPage(): List<T> {
        val loadResult =
            load(PagingSource.LoadParams.Refresh(key = null, loadSize = PAGE_SIZE, placeholdersEnabled = false))
        return (loadResult as PagingSource.LoadResult.Page<Int, T>).data
    }

    private fun metadata(
        resource: Resource,
        name: String,
    ) = ResourceMetadata(
        resourceId = resource.resourceId,
        metadataJson = """{"name":"$name"}""",
        name = name,
        username = null,
        description = null,
        customFieldsKeys = null,
    )

    private companion object {
        private const val PASSWORD_DESCRIPTION_SLUG = "password-description"
        private const val PAGE_SIZE = 10

        private val RESOURCE_TYPE =
            ResourceType(
                resourceTypeId = "1",
                name = "password-description",
                slug = PASSWORD_DESCRIPTION_SLUG,
                deleted = null,
            )

        private fun resource(
            resourceId: String,
            modified: ZonedDateTime,
        ) = Resource(
            resourceId = resourceId,
            folderId = null,
            resourcePermission = Permission.READ,
            resourceTypeId = "1",
            favouriteId = null,
            modified = modified,
            expiry = null,
            metadataKeyId = null,
            metadataKeyType = null,
            updateState = ResourceUpdateState.UPDATED,
        )

        private val NOW = ZonedDateTime.now()

        private val RESOURCE_ZEBRA = resource(resourceId = "1", modified = NOW.plusDays(3))
        private val RESOURCE_BANANA = resource(resourceId = "2", modified = NOW.plusDays(2))
        private val RESOURCE_APPLE_LOWERCASE = resource(resourceId = "3", modified = NOW.plusDays(1))
        private val RESOURCE_APPLE_CAPITALIZED = resource(resourceId = "4", modified = NOW)

        private fun folder(
            folderId: String,
            name: String,
            parentId: String?,
            modified: ZonedDateTime,
        ) = Folder(
            folderId = folderId,
            name = name,
            permission = Permission.READ,
            parentId = parentId,
            isShared = false,
            modified = modified,
            updateState = FolderUpdateState.UPDATED,
        )

        private val ROOT_FOLDER_ZEBRA =
            folder(folderId = "1", name = "Zebra", parentId = null, modified = NOW.plusDays(3))
        private val ROOT_FOLDER_BANANA =
            folder(folderId = "2", name = "banana", parentId = null, modified = NOW.plusDays(2))
        private val ROOT_FOLDER_APPLE_LOWERCASE =
            folder(folderId = "3", name = "apple", parentId = null, modified = NOW)
        private val ROOT_FOLDER_APPLE_CAPITALIZED =
            folder(folderId = "4", name = "Apple", parentId = null, modified = NOW.plusDays(1))

        private val FOLDER_BANANA =
            folder(folderId = "banana", name = "banana", parentId = null, modified = NOW.plusDays(2))
        private val FOLDER_CHERRY =
            folder(folderId = "cherry", name = "cherry", parentId = null, modified = NOW.plusDays(1))
        private val FOLDER_ZEBRA =
            folder(folderId = "zebra", name = "zebra", parentId = FOLDER_BANANA.folderId, modified = NOW.plusDays(3))
        private val FOLDER_APPLE =
            folder(folderId = "apple", name = "apple", parentId = FOLDER_BANANA.folderId, modified = NOW)
        private val FOLDER_AARDVARK =
            folder(folderId = "aardvark", name = "aardvark", parentId = FOLDER_ZEBRA.folderId, modified = NOW.plusDays(4))
        private val NESTED_FOLDERS = listOf(FOLDER_BANANA, FOLDER_CHERRY, FOLDER_ZEBRA, FOLDER_APPLE, FOLDER_AARDVARK)
    }
}
