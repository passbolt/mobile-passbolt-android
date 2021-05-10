package com.passbolt.mobile.android.service.registration.data

import com.passbolt.mobile.android.dto.request.NextPageRequestDto
import com.passbolt.mobile.android.dto.response.NextPageResponseDto
import com.passbolt.mobile.android.dto.response.BaseResponse
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

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

internal interface RegistrationApi {

    @PUT(NEXT_PAGE)
    suspend fun putNextPage(
        @Path(UUID) uuid: String,
        @Path(AUTH_TOKEN) authToken: String,
        @Body pageRequestDto: NextPageRequestDto
    ): BaseResponse<NextPageResponseDto>

    private companion object {
        private const val UUID = "uuid"
        private const val AUTH_TOKEN = "AUTH_TOKEN"
        private const val NEXT_PAGE = "mobile/transfers/{$UUID}/{$AUTH_TOKEN}.json"
    }
}
