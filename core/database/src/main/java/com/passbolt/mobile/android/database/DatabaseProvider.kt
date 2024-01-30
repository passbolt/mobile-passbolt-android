package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room.Room
import com.passbolt.mobile.android.database.migrations.Migration10to11
import com.passbolt.mobile.android.database.migrations.Migration11to12
import com.passbolt.mobile.android.database.migrations.Migration12to13
import com.passbolt.mobile.android.database.migrations.Migration13to14
import com.passbolt.mobile.android.database.migrations.Migration1to2
import com.passbolt.mobile.android.database.migrations.Migration2to3
import com.passbolt.mobile.android.database.migrations.Migration3to4
import com.passbolt.mobile.android.database.migrations.Migration4to5
import com.passbolt.mobile.android.database.migrations.Migration5to6
import com.passbolt.mobile.android.database.migrations.Migration6to7
import com.passbolt.mobile.android.database.migrations.Migration7to8
import com.passbolt.mobile.android.database.migrations.Migration8to9
import com.passbolt.mobile.android.database.migrations.Migration9to10
import com.passbolt.mobile.android.storage.usecase.database.GetResourcesDatabasePassphraseUseCase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
class DatabaseProvider(
    private val getResourcesDatabasePassphraseUseCase: GetResourcesDatabasePassphraseUseCase,
    private val context: Context
) {

    @Volatile
    private var instance: HashMap<String, ResourceDatabase?> = hashMapOf()

    fun get(userId: String): ResourceDatabase {
        val currentUser = hashString(userId)
        instance[currentUser]?.let {
            return it
        }
        val passphrase = getResourcesDatabasePassphraseUseCase.execute(Unit).passphrase
        val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
        val newInstance = Room.databaseBuilder(
            context,
            ResourceDatabase::class.java, "${currentUser}_$RESOURCE_DATABASE_NAME"
        )
            .addMigrations(
                Migration1to2, Migration2to3, Migration3to4, Migration4to5, Migration5to6,
                Migration6to7, Migration7to8, Migration8to9, Migration9to10, Migration10to11,
                Migration11to12, Migration12to13, Migration13to14
            )
            .openHelperFactory(factory)
            .build()

        instance[currentUser] = newInstance
        return newInstance
    }

    suspend fun delete(userId: String) {
        val currentUser = hashString(userId)
        if (currentUser in instance.keys) {
            suspendCoroutine { continuation ->
                Thread {
                    instance[currentUser]?.clearAllTables()
                    continuation.resume(Unit)
                }.start()
            }
            instance.remove(currentUser)
        }
    }

    private fun hashString(input: String, algorithm: String = "SHA-256"): String {
        return MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("") { str, value -> str + "%02x".format(value) }
    }

    companion object {
        private const val RESOURCE_DATABASE_NAME = "resources.db"
    }
}
