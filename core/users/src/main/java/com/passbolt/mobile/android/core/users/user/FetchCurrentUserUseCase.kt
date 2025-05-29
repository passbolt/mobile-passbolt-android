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

package com.passbolt.mobile.android.core.users.user

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.passboltapi.users.UsersRepository
import com.passbolt.mobile.android.ui.UserModel

class FetchCurrentUserUseCase(
    private val usersRepository: UsersRepository,
    private val userModelMapper: UsersModelMapper,
) : AsyncUseCase<Unit, FetchCurrentUserUseCase.Output> {
    override suspend fun execute(input: Unit): Output =
        when (val result = usersRepository.getMyProfile()) {
            is NetworkResult.Failure.NetworkError -> Output.Failure(result, result.exception.message)
            is NetworkResult.Failure.ServerError -> Output.Failure(result, result.headerMessage)
            is NetworkResult.Success -> Output.Success(userModelMapper.map(result.value))
        }

    sealed class Output : AuthenticatedUseCaseOutput {
        override val authenticationState: AuthenticationState
            get() =
                when {
                    this is Failure<*> && this.response.isUnauthorized -> {
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    }
                    this is Failure<*> && this.response.isMfaRequired -> {
                        val providers = MfaTypeProvider.get(this.response)
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Mfa(providers),
                        )
                    }
                    else -> {
                        AuthenticationState.Authenticated
                    }
                }

        data class Success(
            val userModel: UserModel,
        ) : Output()

        data class Failure<T : Any>(
            val response: NetworkResult.Failure<T>,
            val message: String?,
        ) : Output()
    }
}
