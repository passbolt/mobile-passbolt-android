package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import com.passbolt.mobile.android.database.impl.groups.GroupsDao
import com.passbolt.mobile.android.entity.group.GroupUpdateState
import com.passbolt.mobile.android.entity.group.UsersGroup
import kotlinx.coroutines.runBlocking
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

class GroupsSearchTest {
    private lateinit var groupsDao: GroupsDao
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
        groupsDao = db.groupsDao()

        runBlocking {
            groupsDao.insertAll(listOf(MARKETING, DEVELOPERS, ACCOUNTING))
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAllGroupsAreReturnedOrderedByNameWhenQueryIsNull() {
        runBlocking {
            val groups = groupsDao.getAll(ftsQuery = null)

            assertThat(groups.map { it.groupId })
                .containsExactly(ACCOUNTING.groupId, DEVELOPERS.groupId, MARKETING.groupId)
                .inOrder()
        }
    }

    @Test
    fun testGroupsAreMatchedByNamePrefix() {
        runBlocking {
            val groups = groupsDao.getAll(ftsQuery = "dev*")

            assertThat(groups.map { it.groupId }).containsExactly(DEVELOPERS.groupId)
        }
    }

    @Test
    fun testMatchingIsCaseInsensitive() {
        runBlocking {
            val groups = groupsDao.getAll(ftsQuery = "MARK*")

            assertThat(groups.map { it.groupId }).containsExactly(MARKETING.groupId)
        }
    }

    @Test
    fun testMatchingIgnoresDiacritics() {
        runBlocking {
            val withoutDiacritics = groupsDao.getAll(ftsQuery = "comptabilite*")
            val upperCaseWithDiacritics = groupsDao.getAll(ftsQuery = "COMPTABILITÉ*")

            assertThat(withoutDiacritics.map { it.groupId }).containsExactly(ACCOUNTING.groupId)
            assertThat(upperCaseWithDiacritics.map { it.groupId }).containsExactly(ACCOUNTING.groupId)
        }
    }

    @Test
    fun testNoGroupsAreReturnedForNonMatchingQuery() {
        runBlocking {
            val groups = groupsDao.getAll(ftsQuery = "nonexistent*")

            assertThat(groups).isEmpty()
        }
    }

    @Test
    fun testUpdatedGroupIsSearchableByNewName() {
        runBlocking {
            groupsDao.upsertAll(listOf(DEVELOPERS.copy(name = "Engineering")))

            assertThat(groupsDao.getAll(ftsQuery = "engineer*").map { it.groupId }).containsExactly(DEVELOPERS.groupId)
            assertThat(groupsDao.getAll(ftsQuery = "dev*")).isEmpty()
        }
    }

    private companion object {
        val MARKETING = UsersGroup(groupId = "marketing", name = "Marketing", updateState = GroupUpdateState.UPDATED)
        val DEVELOPERS = UsersGroup(groupId = "developers", name = "Developers", updateState = GroupUpdateState.UPDATED)
        val ACCOUNTING = UsersGroup(groupId = "accounting", name = "Comptabilité", updateState = GroupUpdateState.UPDATED)
    }
}
