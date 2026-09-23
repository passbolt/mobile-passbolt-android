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

package com.passbolt.mobile.android.feature.authentication.auth

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class OneTimeSessionRefreshRetryTest {
    private val refreshSessionUseCase = mock<RefreshSessionUseCase>()
    private val getSessionUseCase = mock<GetSessionUseCase>()

    private val retry = OneTimeSessionRefreshRetry(refreshSessionUseCase, getSessionUseCase)

    @Test
    fun `should return first result without refreshing when not unauthorized`() =
        runTest {
            val calls = mutableListOf<String>()

            val result =
                retry.execute(TOKEN, isUnauthorized = { it == UNAUTHORIZED }) {
                    calls += it
                    OK
                }

            assertThat(result).isEqualTo(OK)
            assertThat(calls).containsExactly(TOKEN)
            verifyNoInteractions(refreshSessionUseCase)
        }

    @Test
    fun `should refresh and retry once with refreshed token when unauthorized`() =
        runTest {
            whenever(refreshSessionUseCase.execute(Unit)) doReturn RefreshSessionUseCase.Output.Success
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(REFRESHED_TOKEN, null, null)
            val calls = mutableListOf<String>()

            val result =
                retry.execute(TOKEN, isUnauthorized = { it == UNAUTHORIZED }) {
                    calls += it
                    if (it == TOKEN) UNAUTHORIZED else OK
                }

            assertThat(result).isEqualTo(OK)
            assertThat(calls).containsExactly(TOKEN, REFRESHED_TOKEN).inOrder()
        }

    @Test
    fun `should return unauthorized after single retry when refreshed token is still rejected`() =
        runTest {
            whenever(refreshSessionUseCase.execute(Unit)) doReturn RefreshSessionUseCase.Output.Success
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(REFRESHED_TOKEN, null, null)
            val calls = mutableListOf<String>()

            val result =
                retry.execute(TOKEN, isUnauthorized = { it == UNAUTHORIZED }) {
                    calls += it
                    UNAUTHORIZED
                }

            assertThat(result).isEqualTo(UNAUTHORIZED)
            assertThat(calls).hasSize(2)
            verify(refreshSessionUseCase).execute(Unit)
        }

    @Test
    fun `should return unauthorized without retry when refresh fails`() =
        runTest {
            whenever(refreshSessionUseCase.execute(Unit)) doReturn RefreshSessionUseCase.Output.Failure
            val calls = mutableListOf<String>()

            val result =
                retry.execute(TOKEN, isUnauthorized = { it == UNAUTHORIZED }) {
                    calls += it
                    UNAUTHORIZED
                }

            assertThat(result).isEqualTo(UNAUTHORIZED)
            assertThat(calls).containsExactly(TOKEN)
            verifyNoInteractions(getSessionUseCase)
        }

    @Test
    fun `should return unauthorized without retry when refreshed session has no token`() =
        runTest {
            whenever(refreshSessionUseCase.execute(Unit)) doReturn RefreshSessionUseCase.Output.Success
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, null, null)
            val calls = mutableListOf<String>()

            val result =
                retry.execute(TOKEN, isUnauthorized = { it == UNAUTHORIZED }) {
                    calls += it
                    UNAUTHORIZED
                }

            assertThat(result).isEqualTo(UNAUTHORIZED)
            assertThat(calls).containsExactly(TOKEN)
        }

    private companion object {
        const val TOKEN = "token"
        const val REFRESHED_TOKEN = "refreshed-token"
        const val OK = "ok"
        const val UNAUTHORIZED = "unauthorized"
    }
}
