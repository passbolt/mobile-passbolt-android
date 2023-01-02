package com.passbolt.mobile.android.passboltapi.registration

import com.passbolt.mobile.android.dto.request.CreateTransferRequestDto
import com.passbolt.mobile.android.dto.request.UpdateTransferRequestDto
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.CreateTransferResponseDto
import com.passbolt.mobile.android.dto.response.TransferResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

internal interface MobileTransferApi {

    @POST(MOBILE_TRANSFERS)
    suspend fun createTransfer(
        @Body createTransferRequest: CreateTransferRequestDto
    ): BaseResponse<CreateTransferResponseDto>

    @GET(TRANSFER_BY_ID)
    suspend fun viewTransfer(
        @Header("Authorization") authToken: String,
        @Path(PATH_UUID) uuid: String
    ): BaseResponse<TransferResponseDto>

    @PUT(TRANSFER_BY_ID_AND_AUTH_TOKEN)
    suspend fun updateTransfer(
        @Path(PATH_UUID) uuid: String,
        @Path(PATH_AUTH_TOKEN) authToken: String,
        @Body pageRequestDto: UpdateTransferRequestDto,
        @Query(USER_PROFILE_INFO) userProfile: String?
    ): BaseResponse<TransferResponseDto>

    private companion object {
        private const val PATH_UUID = "uuid"
        private const val PATH_AUTH_TOKEN = "AUTH_TOKEN"

        private const val USER_PROFILE_INFO = "contain[user.profile]"

        private const val MOBILE_TRANSFERS = "mobile/transfers.json"
        private const val TRANSFER_BY_ID_AND_AUTH_TOKEN = "mobile/transfers/{$PATH_UUID}/{$PATH_AUTH_TOKEN}.json"
        private const val TRANSFER_BY_ID = "mobile/transfers/{$PATH_UUID}.json"
    }
}
