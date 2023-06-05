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

package com.passbolt.mobile.android.core.secrets.usecase.decrypt

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import retrofit2.HttpException
import java.net.HttpURLConnection

class SecretInteractor(
    private val fetchSecretUseCase: FetchSecretUseCase,
    private val decryptSecretUseCase: DecryptSecretUseCase
) {

    suspend fun fetchAndDecrypt(resourceId: String): Output =
        when (val response = fetchSecretUseCase.execute(FetchSecretUseCase.Input(resourceId))) {
            is FetchSecretUseCase.Output.EncryptedSecret -> decrypt(response.encryptedSecret)
            is FetchSecretUseCase.Output.Failure -> Output.FetchFailure(response.exception)
        }

    private suspend fun decrypt(encryptedSecret: String): Output =
        when (val output = decryptSecretUseCase.execute(DecryptSecretUseCase.Input(encryptedSecret))) {
            is DecryptSecretUseCase.Output.DecryptedSecret -> Output.Success(output.decryptedSecret)
            is DecryptSecretUseCase.Output.Failure -> Output.DecryptFailure(output.exception)
            is DecryptSecretUseCase.Output.Unauthorized -> Output.Unauthorized(output.reason)
        }

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = if (this is FetchFailure &&
                (this.exception as? HttpException)?.code() == HttpURLConnection.HTTP_UNAUTHORIZED
            ) {
                AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
            } else if (this is Unauthorized) {
                AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Passphrase)
            } else {
                AuthenticationState.Authenticated
            }

        data class FetchFailure(val exception: Exception) : Output()

        data class DecryptFailure(val error: OpenPgpError) : Output()

        data class Unauthorized(val reason: UnauthenticatedReason) : Output()

        data class Success(val decryptedSecret: ByteArray) : Output() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Success

                if (!decryptedSecret.contentEquals(other.decryptedSecret)) return false

                return true
            }

            override fun hashCode(): Int {
                return decryptedSecret.contentHashCode()
            }
        }
    }
}
