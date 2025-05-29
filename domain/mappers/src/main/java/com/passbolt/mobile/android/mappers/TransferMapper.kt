package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.request.StatusRequest
import com.passbolt.mobile.android.dto.request.UpdateTransferRequestDto
import com.passbolt.mobile.android.dto.response.CreateTransferResponseDto
import com.passbolt.mobile.android.dto.response.StatusResponse
import com.passbolt.mobile.android.dto.response.TransferResponseDto
import com.passbolt.mobile.android.ui.CreateTransferModel
import com.passbolt.mobile.android.ui.Status
import com.passbolt.mobile.android.ui.TransferModel
import com.passbolt.mobile.android.ui.UpdateTransferModel

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
class TransferMapper {
    fun mapRequestToDto(
        currentPage: Int,
        status: Status,
    ): UpdateTransferRequestDto = UpdateTransferRequestDto(currentPage, mapStatus(status))

    fun mapUpdateResponseToUi(pageResponseDto: TransferResponseDto): UpdateTransferModel =
        UpdateTransferModel(
            id = pageResponseDto.id.toString(),
            firstName = pageResponseDto.user?.profile?.firstName,
            lastName = pageResponseDto.user?.profile?.lastName,
            email = pageResponseDto.user?.email,
            avatarUrl =
                pageResponseDto.user
                    ?.profile
                    ?.avatar
                    ?.url
                    ?.medium,
        )

    fun mapViewResponseToUi(transfer: TransferResponseDto): TransferModel =
        TransferModel(
            id = transfer.id.toString(),
            status = mapStatus(transfer.status),
            currentPage = transfer.currentPage,
            totalPages = transfer.totalPages,
            hash = transfer.hash,
        )

    fun mapCreateResponseToUi(transfer: CreateTransferResponseDto): CreateTransferModel =
        CreateTransferModel(
            id = transfer.id.toString(),
            status = mapStatus(transfer.status),
            currentPage = transfer.currentPage,
            totalPages = transfer.totalPages,
            hash = transfer.hash,
            authenticationToken = transfer.authToken.token,
        )

    private fun mapStatus(status: StatusResponse) =
        when (status) {
            StatusResponse.ERROR -> Status.ERROR
            StatusResponse.IN_PROGRESS -> Status.IN_PROGRESS
            StatusResponse.COMPLETE -> Status.COMPLETE
            StatusResponse.CANCEL -> Status.CANCEL
            StatusResponse.START -> Status.START
        }

    private fun mapStatus(status: Status) =
        when (status) {
            Status.ERROR -> StatusRequest.ERROR
            Status.IN_PROGRESS -> StatusRequest.IN_PROGRESS
            Status.COMPLETE -> StatusRequest.COMPLETE
            Status.CANCEL -> StatusRequest.CANCEL
            Status.START -> StatusRequest.START
        }
}
