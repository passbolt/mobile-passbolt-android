package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.time.TimeProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicRsaKeyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.measureTimedValue

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
class ServerKeysWarmupCache(
    private val fetchServerPublicPgpKeyUseCase: FetchServerPublicPgpKeyUseCase,
    private val fetchServerPublicRsaKeyUseCase: FetchServerPublicRsaKeyUseCase,
    private val timeProvider: TimeProvider,
    coroutineLaunchContext: CoroutineLaunchContext,
) : ServerKeysWarmup {
    private val scope = CoroutineScope(SupervisorJob() + coroutineLaunchContext.io)
    private val stateLock = ReentrantLock()
    private var current: Entry? = null

    private data class Entry(
        val userId: String,
        val deferred: Deferred<ServerKeysResult>,
    )

    override fun warmUp(userId: String) {
        stateLock.withLock {
            current?.deferred?.cancel()
            current = Entry(userId, scope.async { fetchBoth() })
        }
        Timber.d("Server keys warm-up started")
    }

    override suspend fun fetchOrAwait(userId: String): ServerKeysResult {
        val warmed =
            stateLock.withLock {
                val entry = current
                current = null
                if (entry?.userId == userId) {
                    entry.deferred
                } else {
                    entry?.deferred?.cancel()
                    null
                }
            }
        return if (warmed != null) {
            Timber.d("Using warmed-up server keys")
            warmed.await()
        } else {
            Timber.d("No warm-up available - fetching server keys directly")
            fetchBoth()
        }
    }

    private suspend fun fetchBoth(): ServerKeysResult =
        coroutineScope {
            val pgp = async { measureTimedValue { fetchServerPublicPgpKeyUseCase.execute(Unit) } }
            val rsa = async { fetchServerPublicRsaKeyUseCase.execute(Unit) }
            val timedPgp = pgp.await()
            val deviceTimeAtFetchMillis = timeProvider.getCurrentEpochMillis()
            ServerKeysResult(timedPgp, rsa.await(), deviceTimeAtFetchMillis)
        }
}
