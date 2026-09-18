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

package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.time.TimeProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicRsaKeyUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ServerKeysWarmupCacheTest {
    private val pgpUseCase = mock<FetchServerPublicPgpKeyUseCase>()
    private val rsaUseCase = mock<FetchServerPublicRsaKeyUseCase>()
    private val timeProvider = mock<TimeProvider>()
    private lateinit var cache: ServerKeysWarmupCache

    @Before
    fun setUp() {
        whenever(timeProvider.getCurrentEpochMillis()) doReturn DEVICE_TIME_MILLIS
        cache = ServerKeysWarmupCache(pgpUseCase, rsaUseCase, timeProvider, TestCoroutineLaunchContext())
    }

    @Test
    fun `warm-up then fetchOrAwait for same user invokes fetch once`() =
        runTest {
            stubSuccess()

            cache.warmUp(USER_ID)
            val result = cache.fetchOrAwait(USER_ID)

            assertThat((result.timedPgp.value as FetchServerPublicPgpKeyUseCase.Output.Success).publicKey)
                .isEqualTo(PGP_KEY)
            assertThat((result.rsa as FetchServerPublicRsaKeyUseCase.Output.Success).rsaKey)
                .isEqualTo(RSA_KEY)
            verify(pgpUseCase, times(1)).execute(any())
            verify(rsaUseCase, times(1)).execute(any())
        }

    @Test
    fun `fetchOrAwait without prior warm-up fetches directly`() =
        runTest {
            stubSuccess()

            cache.fetchOrAwait(USER_ID)

            verify(pgpUseCase, times(1)).execute(any())
            verify(rsaUseCase, times(1)).execute(any())
        }

    @Test
    fun `warm-up for a different user invalidates the previous one`() =
        runTest {
            stubSuccess()

            cache.warmUp(USER_ID)
            cache.warmUp(OTHER_USER_ID)
            cache.fetchOrAwait(OTHER_USER_ID)

            verify(pgpUseCase, times(2)).execute(any())
            verify(rsaUseCase, times(2)).execute(any())
        }

    // Guards against the staleness bug: an unconsumed warm-up (race-orphaned, or left over
    // when the user backed out of the auth screen) must not be reused by the next sign-in.
    @Test
    fun `warm-up always cancels any prior fetch for the same user and starts fresh`() =
        runTest {
            stubSuccess()

            cache.warmUp(USER_ID)
            cache.warmUp(USER_ID)
            cache.fetchOrAwait(USER_ID)

            verify(pgpUseCase, times(2)).execute(any())
            verify(rsaUseCase, times(2)).execute(any())
        }

    @Test
    fun `fetchOrAwait is single-shot - second call re-fetches`() =
        runTest {
            stubSuccess()

            cache.warmUp(USER_ID)
            cache.fetchOrAwait(USER_ID)
            cache.fetchOrAwait(USER_ID)

            verify(pgpUseCase, times(2)).execute(any())
            verify(rsaUseCase, times(2)).execute(any())
        }

    @Test
    fun `fetchOrAwait for a user different from warm-up discards the warm-up`() =
        runTest {
            stubSuccess()

            cache.warmUp(USER_ID)
            cache.fetchOrAwait(OTHER_USER_ID)

            verify(pgpUseCase, times(2)).execute(any())
            verify(rsaUseCase, times(2)).execute(any())
        }

    @Test
    fun `fetchOrAwait propagates exceptions thrown by the underlying use case`() =
        runTest {
            whenever(pgpUseCase.execute(any())) doThrow IllegalStateException("network blew up")
            whenever(rsaUseCase.execute(any())) doReturn
                FetchServerPublicRsaKeyUseCase.Output.Success(RSA_KEY)

            assertFailsWith<IllegalStateException> {
                cache.fetchOrAwait(USER_ID)
            }
        }

    @Test
    fun `fetchOrAwait captures the device time at fetch`() =
        runTest {
            stubSuccess()

            val result = cache.fetchOrAwait(USER_ID)

            assertThat(result.deviceTimeAtFetchMillis).isEqualTo(DEVICE_TIME_MILLIS)
        }

    private suspend fun stubSuccess() {
        whenever(pgpUseCase.execute(any())) doReturn
            FetchServerPublicPgpKeyUseCase.Output.Success(PGP_KEY, FINGERPRINT, SERVER_TIME)
        whenever(rsaUseCase.execute(any())) doReturn
            FetchServerPublicRsaKeyUseCase.Output.Success(RSA_KEY)
    }

    private companion object {
        const val USER_ID = "user-1"
        const val OTHER_USER_ID = "user-2"
        const val PGP_KEY = "pgp-key"
        const val RSA_KEY = "rsa-key"
        const val FINGERPRINT = "fingerprint"
        const val SERVER_TIME = 1_700_000_000L
        const val DEVICE_TIME_MILLIS = 1_700_000_050_000L
    }
}
