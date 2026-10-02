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
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnDelegatedTokenVerifier
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class VerifyWebauthnDelegatedTokenLocallyUseCaseTest {
    private val getServerPublicRsaKeyUseCase = mock<GetServerPublicRsaKeyUseCase>()
    private val getAccountDataUseCase = mock<GetAccountDataUseCase>()
    private val verifier = mock<WebauthnDelegatedTokenVerifier>()

    private val useCase =
        VerifyWebauthnDelegatedTokenLocallyUseCase(
            getServerPublicRsaKeyUseCase = getServerPublicRsaKeyUseCase,
            getAccountDataUseCase = getAccountDataUseCase,
            webauthnDelegatedTokenVerifier = verifier,
        )

    @Before
    fun setUp() {
        whenever(getServerPublicRsaKeyUseCase.execute(UserIdInput(USER_ID))) doReturn
            GetServerPublicRsaKeyUseCase.Output(RSA_KEY)
        whenever(getAccountDataUseCase.execute(UserIdInput(USER_ID))) doReturn
            GetAccountDataUseCase.Output(
                firstName = null,
                lastName = null,
                email = null,
                avatarUrl = null,
                url = ACCOUNT_URL,
                serverId = null,
                label = null,
                role = null,
            )
    }

    @Test
    fun `should return valid when verifier accepts the token`() {
        whenever(verifier.verify(any(), any(), any(), any())) doReturn WebauthnDelegatedTokenVerifier.Output.Verified

        val output = useCase.execute(VerifyWebauthnDelegatedTokenLocallyUseCase.Input(USER_ID, TOKEN, STATE))

        assertThat(output).isEqualTo(VerifyWebauthnDelegatedTokenLocallyUseCase.Output.Valid)
        verify(verifier).verify(TOKEN, RSA_KEY, ACCOUNT_URL, STATE)
    }

    @Test
    fun `should return invalid with reason when verifier rejects the token`() {
        whenever(verifier.verify(any(), any(), any(), any())) doReturn
            WebauthnDelegatedTokenVerifier.Output.StateMismatch

        val output = useCase.execute(VerifyWebauthnDelegatedTokenLocallyUseCase.Input(USER_ID, TOKEN, STATE))

        assertThat(output).isEqualTo(
            VerifyWebauthnDelegatedTokenLocallyUseCase.Output.Invalid(
                WebauthnDelegatedTokenVerifier.Output.StateMismatch,
            ),
        )
    }

    @Test
    fun `should return server key unavailable when no key is stored`() {
        whenever(getServerPublicRsaKeyUseCase.execute(UserIdInput(USER_ID))) doReturn
            GetServerPublicRsaKeyUseCase.Output(null)

        val output = useCase.execute(VerifyWebauthnDelegatedTokenLocallyUseCase.Input(USER_ID, TOKEN, STATE))

        assertThat(output).isEqualTo(VerifyWebauthnDelegatedTokenLocallyUseCase.Output.ServerKeyUnavailable)
        verifyNoInteractions(verifier)
    }

    private companion object {
        const val USER_ID = "user-id"
        const val RSA_KEY = "rsa-public-key"
        const val ACCOUNT_URL = "https://passbolt.example.com"
        const val TOKEN = "delegated-token"
        const val STATE = "delegated-state"
    }
}
