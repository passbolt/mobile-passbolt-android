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

package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

// Missing-item CoroutineExceptionHandlers in view models rely on Room throwing IllegalStateException
// for an empty result of a single-row non-null query. Room changed this exception type before
// without documenting it in release notes, so this contract is guarded here against the real generated DAOs.
class EmptyQueryResultExceptionTest {
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
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun emptyResultOfSingleRowResourceQueryShouldThrowIllegalStateException() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { db.resourcesDao().get("missing-resource-id") }
        }
    }

    @Test
    fun emptyResultOfSingleRowFolderQueryShouldThrowIllegalStateException() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { db.foldersDao().get("missing-folder-id") }
        }
    }
}
