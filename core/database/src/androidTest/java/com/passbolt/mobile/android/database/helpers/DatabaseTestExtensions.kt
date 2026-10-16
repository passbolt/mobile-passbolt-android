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

package com.passbolt.mobile.android.database.helpers

import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteDriver
import com.passbolt.mobile.android.database.ResourceDatabase
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver

internal fun testSqlCipherDriver(): SQLiteDriver = TestSqlCipher.driver()

private object TestSqlCipher {
    private const val PASSPHRASE = "database-test-passphrase"

    init {
        System.loadLibrary("sqlcipher")
    }

    fun driver(): SQLiteDriver = SQLCipherDriver(PASSPHRASE.toByteArray(), null, null)
}

internal suspend fun ResourceDatabase.executeSql(sql: String) {
    useWriterConnection { transactor -> transactor.usePrepared(sql) { it.step() } }
}

internal suspend fun ResourceDatabase.countRows(
    sql: String,
    args: Array<String>,
): Int =
    useReaderConnection { transactor ->
        transactor.usePrepared(sql) { statement ->
            args.forEachIndexed { index, arg -> statement.bindText(index + 1, arg) }
            var count = 0
            while (statement.step()) {
                count++
            }
            count
        }
    }
