package com.passbolt.mobile.android.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.passbolt.mobile.android.common.hash.MessageDigestHash
import com.passbolt.mobile.android.database.migrations.Migration10to11
import com.passbolt.mobile.android.database.migrations.Migration11to12
import com.passbolt.mobile.android.database.migrations.Migration12to13
import com.passbolt.mobile.android.database.migrations.Migration13to14
import com.passbolt.mobile.android.database.migrations.Migration14to15
import com.passbolt.mobile.android.database.migrations.Migration15to16
import com.passbolt.mobile.android.database.migrations.Migration16to17
import com.passbolt.mobile.android.database.migrations.Migration17to18
import com.passbolt.mobile.android.database.migrations.Migration18to19
import com.passbolt.mobile.android.database.migrations.Migration19to20
import com.passbolt.mobile.android.database.migrations.Migration1to2
import com.passbolt.mobile.android.database.migrations.Migration20to21
import com.passbolt.mobile.android.database.migrations.Migration21to22
import com.passbolt.mobile.android.database.migrations.Migration22to23
import com.passbolt.mobile.android.database.migrations.Migration23to24
import com.passbolt.mobile.android.database.migrations.Migration24to25
import com.passbolt.mobile.android.database.migrations.Migration25to26
import com.passbolt.mobile.android.database.migrations.Migration26to27
import com.passbolt.mobile.android.database.migrations.Migration27to28
import com.passbolt.mobile.android.database.migrations.Migration2to3
import com.passbolt.mobile.android.database.migrations.Migration3to4
import com.passbolt.mobile.android.database.migrations.Migration4to5
import com.passbolt.mobile.android.database.migrations.Migration5to6
import com.passbolt.mobile.android.database.migrations.Migration6to7
import com.passbolt.mobile.android.database.migrations.Migration7to8
import com.passbolt.mobile.android.database.migrations.Migration8to9
import com.passbolt.mobile.android.database.migrations.Migration9to10
import com.passbolt.mobile.android.domain.auth.usecase.GetResourcesDatabasePassphraseUseCase
import kotlinx.coroutines.suspendCancellableCoroutine
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

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
    private val context: Context,
    private val messageDigestHash: MessageDigestHash,
) {
    private val instance = ConcurrentHashMap<String, ResourceDatabase>()

    fun get(userId: String): ResourceDatabase {
        System.loadLibrary("sqlcipher")
        val currentUser = messageDigestHash.sha256(userId)
        return instance.computeIfAbsent(currentUser) {
            try {
                val passphrase = getResourcesDatabasePassphraseUseCase.execute(Unit).passphrase
                val factory = SupportOpenHelperFactory(passphrase.toByteArray(StandardCharsets.UTF_8))
                Room
                    .databaseBuilder(
                        context,
                        ResourceDatabase::class.java,
                        "${currentUser}_$RESOURCE_DATABASE_NAME",
                    ).addMigrations(
                        Migration1to2,
                        Migration2to3,
                        Migration3to4,
                        Migration4to5,
                        Migration5to6,
                        Migration6to7,
                        Migration7to8,
                        Migration8to9,
                        Migration9to10,
                        Migration10to11,
                        Migration11to12,
                        Migration12to13,
                        Migration13to14,
                        Migration14to15,
                        Migration15to16,
                        Migration16to17,
                        Migration17to18,
                        Migration18to19,
                        Migration19to20,
                        Migration20to21,
                        Migration21to22,
                        Migration22to23,
                        Migration23to24,
                        Migration24to25,
                        Migration25to26,
                        Migration26to27,
                        Migration27to28,
                    ).openHelperFactory(factory)
                    .build()
            } catch (e: Exception) {
                Timber.e(e, "Failed to open resources database")
                throw e
            }
        }
    }

    suspend fun <T> inTransaction(
        userId: String,
        block: suspend () -> T,
    ): T = get(userId).withTransaction { block() }

    suspend fun delete(userId: String) {
        Timber.d("Deleting resources database")
        val currentUser = messageDigestHash.sha256(userId)
        if (currentUser in instance.keys) {
            suspendCancellableCoroutine { continuation ->
                Thread {
                    instance[currentUser]?.clearAllTables()
                    continuation.resume(Unit)
                }.start()
            }
            instance.remove(currentUser)
        }
    }

    companion object {
        private const val RESOURCE_DATABASE_NAME = "resources.db"
    }
}
