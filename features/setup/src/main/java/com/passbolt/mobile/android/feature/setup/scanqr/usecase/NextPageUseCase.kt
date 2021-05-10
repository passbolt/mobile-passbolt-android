package com.passbolt.mobile.android.feature.setup.scanqr.usecase

import com.passbolt.mobile.android.common.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.service.registration.RegistrationRepository
import com.passbolt.mobile.android.mappers.NextQrPageMapper
import com.passbolt.mobile.android.ui.Status
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
class NextPageUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val nextQrPageMapper: NextQrPageMapper,
    private val coroutineContext: CoroutineLaunchContext
) : AsyncUseCase<NextPageUseCase.Input, NextPageUseCase.Output> {

    override suspend fun execute(input: Input): Output = withContext(coroutineContext.io) {
        val response = registrationRepository.turnPage(
            input.uuid,
            input.authToken,
            nextQrPageMapper.mapRequestToDto(input.currentPage, input.status)
        )
        when (response) {
            is NetworkResult.Failure -> Output.Failure
            is NetworkResult.Success -> Output.Success(nextQrPageMapper.mapResponseToUi(response.value.body))
        }
    }

    class Input(
        val uuid: String,
        val authToken: String,
        val currentPage: Int,
        val status: Status
    )

    sealed class Output {
        class Success(
            val id: String
        ) : Output()

        object Failure : Output()
    }
}
