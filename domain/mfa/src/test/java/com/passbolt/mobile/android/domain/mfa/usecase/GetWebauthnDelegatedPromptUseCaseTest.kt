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
package com.passbolt.mobile.android.domain.mfa.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.mfa.MfaRepository
import com.passbolt.mobile.android.domain.mfa.model.WebauthnDelegatedPrompt
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

class GetWebauthnDelegatedPromptUseCaseTest {
    private val mfaRepository = mock<MfaRepository>()

    private val useCase = GetWebauthnDelegatedPromptUseCase(mfaRepository)

    @Test
    fun `should return ceremony token when prompt is found`() =
        runTest {
            whenever(mfaRepository.getWebauthnDelegatedPrompt(JWT)) doReturn
                DomainResult.Finished(WebauthnDelegatedPrompt.Succeeded(CEREMONY_TOKEN))

            val output = useCase.execute(GetWebauthnDelegatedPromptUseCase.Input(JWT))

            assertIs<GetWebauthnDelegatedPromptUseCase.Output.Success>(output)
            assertThat(output.ceremonyToken).isEqualTo(CEREMONY_TOKEN)
            verify(mfaRepository).getWebauthnDelegatedPrompt(JWT)
        }

    @Test
    fun `should return unauthorized when prompt is unauthorized`() =
        runTest {
            whenever(mfaRepository.getWebauthnDelegatedPrompt(JWT)) doReturn
                DomainResult.Finished(WebauthnDelegatedPrompt.Unauthorized)

            val output = useCase.execute(GetWebauthnDelegatedPromptUseCase.Input(JWT))

            assertIs<GetWebauthnDelegatedPromptUseCase.Output.Unauthorized>(output)
        }

    @Test
    fun `should return network failure with error code on other failure`() =
        runTest {
            whenever(mfaRepository.getWebauthnDelegatedPrompt(JWT)) doReturn
                DomainResult.Finished(WebauthnDelegatedPrompt.OtherFailure(400))

            val output = useCase.execute(GetWebauthnDelegatedPromptUseCase.Input(JWT))

            assertIs<GetWebauthnDelegatedPromptUseCase.Output.NetworkFailure>(output)
            assertThat(output.errorCode).isEqualTo(400)
        }

    @Test
    fun `should return failure when repository result is incomplete`() =
        runTest {
            whenever(mfaRepository.getWebauthnDelegatedPrompt(JWT)) doReturn DomainResult.Incomplete.Unauthorized

            val output = useCase.execute(GetWebauthnDelegatedPromptUseCase.Input(JWT))

            assertIs<GetWebauthnDelegatedPromptUseCase.Output.Failure>(output)
            assertThat(output.incomplete).isEqualTo(DomainResult.Incomplete.Unauthorized)
        }

    private companion object {
        const val JWT = "jwt"
        const val CEREMONY_TOKEN = "ceremony-token"
    }
}
