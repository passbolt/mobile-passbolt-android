package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.ServerPgpResponseDto
import com.passbolt.mobile.android.passboltapi.auth.AuthRepository

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
class FetchServerPublicPgpKeyUseCase(
    private val authRepository: AuthRepository
) : AsyncUseCase<Unit, FetchServerPublicPgpKeyUseCase.Output> {

    override suspend fun execute(input: Unit): Output =
        when (val result = authRepository.getServerPublicPgpKey()) {
            is NetworkResult.Failure -> Output.Failure(result)
            is NetworkResult.Success -> Output.Success(
                result.value.body.keydata,
                result.value.body.fingerprint,
                result.value.header.serverTime
            )
        }

    sealed class Output {

        data class Success(
            val publicKey: String,
            val fingerprint: String,
            val serverTime: Long
        ) : Output()

        data class Failure(val error: NetworkResult.Failure<BaseResponse<ServerPgpResponseDto>>) : Output()
    }
}
