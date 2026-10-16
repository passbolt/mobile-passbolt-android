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

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnDelegatedTokenVerifier
import timber.log.Timber

class VerifyWebauthnDelegatedTokenLocallyUseCase(
    private val getServerPublicRsaKeyUseCase: GetServerPublicRsaKeyUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val webauthnDelegatedTokenVerifier: WebauthnDelegatedTokenVerifier,
) : UseCase<VerifyWebauthnDelegatedTokenLocallyUseCase.Input, VerifyWebauthnDelegatedTokenLocallyUseCase.Output> {
    override fun execute(input: Input): Output {
        val rsaKey = getServerPublicRsaKeyUseCase.execute(UserIdInput(input.userId)).rsaKey
        if (rsaKey == null) {
            Timber.e("Server public key is not available, cannot verify webauthn delegated token")
            return Output.ServerKeyUnavailable
        }
        val verification =
            webauthnDelegatedTokenVerifier.verify(
                delegatedToken = input.delegatedToken,
                rsaPublicKey = rsaKey,
                expectedIssuer = getAccountDataUseCase.execute(UserIdInput(input.userId)).url,
                expectedState = input.expectedState,
            )
        return if (verification is WebauthnDelegatedTokenVerifier.Output.Verified) {
            Output.Valid
        } else {
            Output.Invalid(verification)
        }
    }

    data class Input(
        val userId: String,
        val delegatedToken: String,
        val expectedState: String,
    )

    sealed class Output {
        data object Valid : Output()

        data class Invalid(
            val reason: WebauthnDelegatedTokenVerifier.Output,
        ) : Output()

        data object ServerKeyUnavailable : Output()
    }
}
