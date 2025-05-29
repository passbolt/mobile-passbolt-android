package com.passbolt.mobile.android.feature.setup.scanqr.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.TransferResponseDto
import com.passbolt.mobile.android.mappers.TransferMapper
import com.passbolt.mobile.android.passboltapi.registration.MobileTransferRepository
import com.passbolt.mobile.android.ui.Status
import com.passbolt.mobile.android.ui.UpdateTransferModel
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
class UpdateTransferUseCase(
    private val mobileTransferRepository: MobileTransferRepository,
    private val transferMapper: TransferMapper,
    private val coroutineContext: CoroutineLaunchContext,
) : AsyncUseCase<UpdateTransferUseCase.Input, UpdateTransferUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        withContext(coroutineContext.io) {
            val response =
                mobileTransferRepository.turnPage(
                    input.uuid,
                    input.authToken,
                    transferMapper.mapRequestToDto(input.currentPage, input.status),
                    if (input.status == Status.COMPLETE) PROFILE_INFO_REQUIRED else null,
                )
            when (response) {
                is NetworkResult.Failure -> Output.Failure(response)
                is NetworkResult.Success -> Output.Success(transferMapper.mapUpdateResponseToUi(response.value.body))
            }
        }

    data class Input(
        val uuid: String,
        val authToken: String,
        val currentPage: Int,
        val status: Status,
    )

    sealed class Output {
        data class Success(
            val updateTransferModel: UpdateTransferModel,
        ) : Output()

        data class Failure(
            val error: NetworkResult.Failure<BaseResponse<TransferResponseDto>>,
        ) : Output()
    }

    companion object {
        private const val PROFILE_INFO_REQUIRED = "1"
    }
}
