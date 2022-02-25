package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.CookieExtractor
import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.mappers.SignInMapper
import com.passbolt.mobile.android.passboltapi.auth.AuthRepository
import java.net.HttpURLConnection

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

typealias SignInFailureType = SignInUseCase.Output.Failure.FailureType

class SignInUseCase(
    private val authRepository: AuthRepository,
    private val signInMapper: SignInMapper,
    private val cookieExtractor: CookieExtractor
) : AsyncUseCase<SignInUseCase.Input, SignInUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        return when (val result = authRepository.signIn(
            signInMapper.mapRequestToDto(input.userId, input.challenge),
            input.mfaToken
        )) {
            is NetworkResult.Failure.NetworkError -> Output.Failure(
                result.headerMessage,
                Output.Failure.FailureType.OTHER
            )
            is NetworkResult.Failure.ServerError -> Output.Failure(
                result.headerMessage,
                getFailureType(result.errorCode)
            )
            is NetworkResult.Success -> {
                result.value.body()?.body?.challenge?.let {
                    Output.Success(it, cookieExtractor.get(result.value, CookieExtractor.MFA_COOKIE))
                } ?: Output.Failure("", Output.Failure.FailureType.OTHER)
            }
        }
    }

    private fun getFailureType(errorCode: Int?) =
        if (errorCode == HttpURLConnection.HTTP_NOT_FOUND) {
            SignInFailureType.ACCOUNT_DOES_NOT_EXIST
        } else {
            SignInFailureType.OTHER
        }

    sealed class Output {
        data class Success(
            val challenge: String,
            val mfaToken: String?
        ) : Output()

        data class Failure(
            val message: String,
            val type: FailureType
        ) : Output() {

            enum class FailureType {
                ACCOUNT_DOES_NOT_EXIST,
                OTHER
            }
        }
    }

    data class Input(
        val userId: String,
        val challenge: String,
        val mfaToken: String?
    )
}
