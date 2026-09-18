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
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.SERVER
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.TIMEOUT
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor.Error
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.TimedValue

class GetAndVerifyServerKeysAndTimeInteractorTest : KoinTest {
    private val mockServerKeysWarmup = mock<ServerKeysWarmup>()
    private val mockSaveServerPublicRsaKeyUseCase = mock<SaveServerPublicRsaKeyUseCase>()
    private val mockIsServerFingerprintCorrectUseCase = mock<IsServerFingerprintCorrectUseCase>()
    private val mockGetAccountDataUseCase = mock<GetAccountDataUseCase>()
    private val mockGopenPgpTimeUpdater = mock<GopenPgpTimeUpdater>()
    private val mockOpenPgp = mock<OpenPgp>()
    private val interactor: GetAndVerifyServerKeysAndTimeInteractor by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    factory { mockServerKeysWarmup }
                    factory { mockSaveServerPublicRsaKeyUseCase }
                    factory { mockIsServerFingerprintCorrectUseCase }
                    factory { mockGetAccountDataUseCase }
                    factory { mockGopenPgpTimeUpdater }
                    factory { mockOpenPgp }
                    factoryOf(::GetAndVerifyServerKeysAndTimeInteractor)
                },
            )
        }

    @Test
    fun `no network on key fetch should map to NoNetwork error`() =
        runTest {
            stubKeyFetchFailure(DomainResult.Incomplete.Error(OFFLINE, null))

            val error = captureError()

            assertThat(error).isEqualTo(Error.NoNetwork)
        }

    @Test
    fun `timeout on key fetch should map to ServerNotReachable error`() =
        runTest {
            stubKeyFetchFailure(DomainResult.Incomplete.Error(TIMEOUT, null))
            val accountData = mock<GetAccountDataUseCase.Output> { on { url }.thenReturn(SERVER_URL) }
            whenever(mockGetAccountDataUseCase.execute(any())).thenReturn(accountData)

            val error = captureError()

            assertThat(error).isEqualTo(Error.ServerNotReachable(SERVER_URL))
        }

    @Test
    fun `other failure on key fetch should map to Generic error`() =
        runTest {
            stubKeyFetchFailure(DomainResult.Incomplete.Error(SERVER, "boom"))

            val error = captureError()

            assertThat(error).isEqualTo(Error.Generic)
        }

    private suspend fun stubKeyFetchFailure(incomplete: DomainResult.Incomplete) {
        whenever(mockServerKeysWarmup.fetchOrAwait(USER_ID)).thenReturn(
            ServerKeysResult(
                timedPgp = TimedValue(FetchServerPublicPgpKeyUseCase.Output.Failure(incomplete), Duration.ZERO),
                rsa = FetchServerPublicRsaKeyUseCase.Output.Failure(incomplete),
                deviceTimeAtFetchMillis = 0L,
            ),
        )
    }

    @Test
    fun `valid computed fingerprint maps to success`() =
        runTest {
            stubKeyFetchSuccess(reportedFingerprint = COMPUTED_FINGERPRINT_UPPER)
            whenever(mockOpenPgp.getKeyFingerprint(PGP_KEY))
                .thenReturn(OpenPgpResult.Result(COMPUTED_FINGERPRINT_LOWER))
            whenever(mockIsServerFingerprintCorrectUseCase.execute(any()))
                .thenReturn(IsServerFingerprintCorrectUseCase.Output(true))

            var success: GetAndVerifyServerKeysAndTimeInteractor.Success? = null
            interactor.getAndVerifyServerKeys(USER_ID, onError = { }, onSuccess = { success = it })

            assertThat(success?.pgpKeyFingerprint).isEqualTo(COMPUTED_FINGERPRINT_UPPER)
            verify(mockSaveServerPublicRsaKeyUseCase).execute(any())
        }

    @Test
    fun `reported fingerprint not matching key data maps to IncorrectServerFingerprint`() =
        runTest {
            stubKeyFetchSuccess(reportedFingerprint = "0000000000000000000000000000000000000000")
            whenever(mockOpenPgp.getKeyFingerprint(PGP_KEY))
                .thenReturn(OpenPgpResult.Result(COMPUTED_FINGERPRINT_LOWER))

            val error = captureError()

            assertThat(error).isEqualTo(Error.IncorrectServerFingerprint(COMPUTED_FINGERPRINT_UPPER))
            verify(mockSaveServerPublicRsaKeyUseCase, never()).execute(any())
        }

    @Test
    fun `computed fingerprint not matching pinned one maps to IncorrectServerFingerprint`() =
        runTest {
            stubKeyFetchSuccess(reportedFingerprint = COMPUTED_FINGERPRINT_UPPER)
            whenever(mockOpenPgp.getKeyFingerprint(PGP_KEY))
                .thenReturn(OpenPgpResult.Result(COMPUTED_FINGERPRINT_LOWER))
            whenever(mockIsServerFingerprintCorrectUseCase.execute(any()))
                .thenReturn(IsServerFingerprintCorrectUseCase.Output(false))

            val error = captureError()

            assertThat(error).isEqualTo(Error.IncorrectServerFingerprint(COMPUTED_FINGERPRINT_UPPER))
            verify(mockSaveServerPublicRsaKeyUseCase, never()).execute(any())
        }

    private suspend fun stubKeyFetchSuccess(reportedFingerprint: String) {
        whenever(mockServerKeysWarmup.fetchOrAwait(USER_ID)).thenReturn(
            ServerKeysResult(
                timedPgp =
                    TimedValue(
                        FetchServerPublicPgpKeyUseCase.Output.Success(
                            publicKey = PGP_KEY,
                            fingerprint = reportedFingerprint,
                            serverTime = 0L,
                        ),
                        Duration.ZERO,
                    ),
                rsa = FetchServerPublicRsaKeyUseCase.Output.Success(RSA_KEY),
                deviceTimeAtFetchMillis = 0L,
            ),
        )
        whenever(mockGopenPgpTimeUpdater.updateTimeIfNeeded(any(), any(), any()))
            .thenReturn(GopenPgpTimeUpdater.Result.TIME_SYNCED)
    }

    private suspend fun captureError(): Error? {
        var captured: Error? = null
        interactor.getAndVerifyServerKeys(USER_ID, onError = { captured = it }, onSuccess = { })
        return captured
    }

    private companion object {
        const val USER_ID = "userId"
        const val SERVER_URL = "https://passbolt.local"
        const val PGP_KEY = "server-pgp-key-data"
        const val RSA_KEY = "server-rsa-key-data"
        const val COMPUTED_FINGERPRINT_LOWER = "63452c7a0ae6fae8c8c309640bd9e2409bc6a569"
        const val COMPUTED_FINGERPRINT_UPPER = "63452C7A0AE6FAE8C8C309640BD9E2409BC6A569"
    }
}
