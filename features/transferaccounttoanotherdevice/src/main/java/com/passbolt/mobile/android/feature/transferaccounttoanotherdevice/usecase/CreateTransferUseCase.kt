package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.request.CreateTransferRequestDto
import com.passbolt.mobile.android.mappers.TransferMapper
import com.passbolt.mobile.android.passboltapi.registration.MobileTransferRepository
import com.passbolt.mobile.android.ui.CreateTransferModel
import kotlinx.coroutines.withContext

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

class CreateTransferUseCase(
    private val mobileTransferRepository: MobileTransferRepository,
    private val transferMapper: TransferMapper,
    private val coroutineContext: CoroutineLaunchContext
) : AsyncUseCase<CreateTransferUseCase.Input, CreateTransferUseCase.Output> {

    override suspend fun execute(input: Input): Output = withContext(coroutineContext.io) {
        when (val response = mobileTransferRepository.createTransfer(
            CreateTransferRequestDto(input.totalPagesCount, input.hash)
        )) {
            is NetworkResult.Failure -> Output.Failure(response)
            is NetworkResult.Success -> Output.Success(transferMapper.mapCreateResponseToUi(response.value))
        }
    }

    data class Input(
        val totalPagesCount: Int,
        val hash: String
    )

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = when {
                this is Failure<*> && this.response.isUnauthorized -> {
                    AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                }
                this is Failure<*> && this.response.isMfaRequired -> {
                    val providers = MfaTypeProvider.get(this.response)
                    AuthenticationState.Unauthenticated(
                        AuthenticationState.Unauthenticated.Reason.Mfa(providers)
                    )
                }
                else -> {
                    AuthenticationState.Authenticated
                }
            }

        data class Success(
            val transfer: CreateTransferModel
        ) : Output()

        class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()
    }
}
