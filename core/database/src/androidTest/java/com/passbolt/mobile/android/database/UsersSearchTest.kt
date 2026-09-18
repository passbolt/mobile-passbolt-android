package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import com.passbolt.mobile.android.database.impl.users.UsersDao
import com.passbolt.mobile.android.entity.user.User
import com.passbolt.mobile.android.entity.user.UserGpgKey
import com.passbolt.mobile.android.entity.user.UserProfile
import com.passbolt.mobile.android.entity.user.UserUpdateState
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

class UsersSearchTest {
    private lateinit var usersDao: UsersDao
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
        usersDao = db.usersDao()

        runBlocking {
            usersDao.insertAll(listOf(BETTY, EDITH, ADA))
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAllUsersAreReturnedOrderedByNameWhenQueryIsNull() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = null)

            assertThat(users.map { it.id })
                .containsExactly(ADA.id, BETTY.id, EDITH.id)
                .inOrder()
        }
    }

    @Test
    fun testMatchingUsersAreOrderedByFirstNameThenLastNameIgnoringCase() {
        runBlocking {
            val adaByron = user(id = "adaByron", userName = "ab@passbolt.com", firstName = "ada", lastName = "Byron")
            usersDao.insertAll(listOf(adaByron))

            val users = usersDao.getAll(ftsQuery = "passbolt*")

            assertThat(users.map { it.id })
                .containsExactly(adaByron.id, ADA.id, BETTY.id, EDITH.id)
                .inOrder()
        }
    }

    @Test
    fun testUsersAreMatchedByFirstNamePrefix() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = "ad*")

            assertThat(users.map { it.id }).containsExactly(ADA.id)
        }
    }

    @Test
    fun testUsersAreMatchedByLastNamePrefix() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = "holber*")

            assertThat(users.map { it.id }).containsExactly(BETTY.id)
        }
    }

    @Test
    fun testUsersAreMatchedByUserName() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = "ec*")

            assertThat(users.map { it.id }).containsExactly(EDITH.id)
        }
    }

    @Test
    fun testMultiWordQueryMatchesAcrossNameColumns() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = "ada love*")

            assertThat(users.map { it.id }).containsExactly(ADA.id)
        }
    }

    @Test
    fun testMatchingIsCaseInsensitiveForUnicodeCharacters() {
        runBlocking {
            val lowerCaseMatches = usersDao.getAll(ftsQuery = "édith*")
            val upperCaseMatches = usersDao.getAll(ftsQuery = "ÉDITH*")

            assertThat(lowerCaseMatches.map { it.id }).containsExactly(EDITH.id)
            assertThat(upperCaseMatches.map { it.id }).containsExactly(EDITH.id)
        }
    }

    @Test
    fun testMatchingIgnoresDiacritics() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = "edith*")

            assertThat(users.map { it.id }).containsExactly(EDITH.id)
        }
    }

    @Test
    fun testNoUsersAreReturnedForNonMatchingQuery() {
        runBlocking {
            val users = usersDao.getAll(ftsQuery = "nonexistent*")

            assertThat(users).isEmpty()
        }
    }

    @Test
    fun testUpdatedUserIsSearchableByNewName() {
        runBlocking {
            usersDao.upsertAll(listOf(ADA.copy(profile = ADA.profile.copy(lastName = "Byron"))))

            assertThat(usersDao.getAll(ftsQuery = "byron*").map { it.id }).containsExactly(ADA.id)
            assertThat(usersDao.getAll(ftsQuery = "lovelace*")).isEmpty()
        }
    }

    private companion object {
        val ADA = user(id = "ada", userName = "ada@passbolt.com", firstName = "Ada", lastName = "Lovelace")
        val BETTY = user(id = "betty", userName = "betty@passbolt.com", firstName = "Betty", lastName = "Holberton")
        val EDITH = user(id = "edith", userName = "ec@passbolt.com", firstName = "Édith", lastName = "Clarke")

        private fun user(
            id: String,
            userName: String,
            firstName: String,
            lastName: String,
        ) = User(
            id = id,
            userName = userName,
            disabled = false,
            profile = UserProfile(firstName = firstName, lastName = lastName, avatarUrl = null),
            gpgKey =
                UserGpgKey(
                    id = "$id-key",
                    armoredKey = "armoredKey",
                    bits = 4096,
                    uid = null,
                    keyId = "keyId",
                    fingerprint = "fingerprint",
                    type = "RSA",
                    expires = null,
                    created = null,
                ),
            updateState = UserUpdateState.UPDATED,
        )
    }
}
