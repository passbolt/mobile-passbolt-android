package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.FOLDER_1
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.FOLDER_2
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.FOLDER_3
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.FOLDER_4
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.FOLDER_5
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.RESOURCE_1
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.RESOURCE_2
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.RESOURCE_3
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.RESOURCE_4
import com.passbolt.mobile.android.database.FoldersSearchTest.SearchFolderStructure.RESOURCE_5
import com.passbolt.mobile.android.database.dao.FoldersDao
import com.passbolt.mobile.android.database.dao.ResourcesDao
import com.passbolt.mobile.android.entity.resource.Folder
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
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

class FoldersSearchTest {

    private lateinit var resourcesDao: ResourcesDao
    private lateinit var foldersDao: FoldersDao
    private lateinit var db: ResourceDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ResourceDatabase::class.java
        ).build()
        resourcesDao = db.resourcesDao()
        foldersDao = db.foldersDao()

        insertTestStructure()
    }

    private fun insertTestStructure() = runBlocking {
        foldersDao.apply {
            insert(listOf(FOLDER_1, FOLDER_2, FOLDER_3, FOLDER_4, FOLDER_5))
        }
        resourcesDao.apply {
            insert(listOf(RESOURCE_1, RESOURCE_2, RESOURCE_3, RESOURCE_4, RESOURCE_5))
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAllSubFoldersShouldBeReturnedForFolderWithId() = runBlocking {
        val subFoldersForFolder1 = foldersDao.getFilteredSubFoldersRecursivelyForFolderWithId(
            FOLDER_1.folderId
        )
        val subFoldersForFolder2 = foldersDao.getFilteredSubFoldersRecursivelyForFolderWithId(
            FOLDER_2.folderId
        )

        assertThat(subFoldersForFolder1.size).isEqualTo(0)

        assertThat(subFoldersForFolder2.size).isEqualTo(2)
        assertThat(subFoldersForFolder2.map { it.folderId }).containsExactly(
            "rootFolder2Folder1Folder1",
            "rootFolder2Folder1Folder1Folder1"
        )
        return@runBlocking
    }

    @Test
    fun testSubfolderWithResourcesCountShouldBeCorrect() = runBlocking {
        val resourcesAndFoldersCountForRootFolder2 = foldersDao.getResourcesAndFoldersCountForFolderWithId(
            FOLDER_2.folderId,
        )

        assertThat(resourcesAndFoldersCountForRootFolder2).isEqualTo(3)

        return@runBlocking
    }

    object SearchFolderStructure {
        /*
        Items below represent the following structure:
        -R root resource 1
        -R root resource 2
        -F root folder 1
        -F root folder 2
            -R root folder 2 resource 1
            -R root folder 2 resource 2
            -F root folder 2 folder 1
                -R root folder 2 folder 1 resource 1
                -F root folder 2 folder 1 folder 1
                    -F root folder 2 folder 1 folder 1 folder 1
         */

        val RESOURCE_1 = Resource(
            "1",
            null,
            "root resource 1",
            Permission.READ,
            null,
            null,
            null,
            "1",
            false,
            ZonedDateTime.now()
        )
        val RESOURCE_2 = Resource(
            "2",
            null,
            "root resource 2",
            Permission.READ,
            null,
            null,
            null,
            "1",
            false,
            ZonedDateTime.now()
        )
        val RESOURCE_3 = Resource(
            "3",
            "rootFolder2",
            "root folder 2 resource 1",
            Permission.READ,
            null,
            null,
            null,
            "1",
            false,
            ZonedDateTime.now()
        )
        val RESOURCE_4 = Resource(
            "4",
            "rootFolder2",
            "root folder 2 resource 2",
            Permission.READ,
            null,
            null,
            null,
            "1",
            false,
            ZonedDateTime.now()
        )
        val RESOURCE_5 = Resource(
            "5",
            "rootFolder2Folder1",
            "root folder 2 folder 1 resource 1",
            Permission.READ,
            null,
            null,
            null,
            "1",
            false,
            ZonedDateTime.now()
        )

        val FOLDER_1 = Folder(
            "rootFolder1",
            "root folder 1",
            Permission.READ,
            null,
            false
        )
        val FOLDER_2 = Folder(
            "rootFolder2",
            "root folder 2",
            Permission.READ,
            null,
            false
        )
        val FOLDER_3 = Folder(
            "rootFolder2Folder1",
            "root folder 2 folder 1",
            Permission.READ,
            "rootFolder2",
            false
        )
        val FOLDER_4 = Folder(
            "rootFolder2Folder1Folder1",
            "root folder 2 folder 1 folder 1",
            Permission.READ,
            "rootFolder2Folder1",
            false
        )
        val FOLDER_5 = Folder(
            "rootFolder2Folder1Folder1Folder1",
            "root folder 2 folder 1 folder 1 folder 1",
            Permission.READ,
            "rootFolder2Folder1Folder1",
            false
        )
    }
}
