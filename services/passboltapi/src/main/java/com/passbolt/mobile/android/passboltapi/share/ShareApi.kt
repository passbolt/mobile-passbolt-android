package com.passbolt.mobile.android.passboltapi.share

import com.passbolt.mobile.android.dto.request.ShareRequest
import com.passbolt.mobile.android.dto.request.SimulateShareRequest
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.SimulateShareResponse
import retrofit2.http.Body
import retrofit2.http.POST
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
internal interface ShareApi {

    @POST(SIMULATE_RESOURCE_SHARE)
    suspend fun simulateShareResource(
        @Path(PATH_RESOURCE_ID) resourceId: String,
        @Body request: SimulateShareRequest
    ): BaseResponse<SimulateShareResponse>

    @PUT(SHARE_RESOURCE)
    suspend fun shareResource(
        @Path(PATH_RESOURCE_ID) resourceId: String,
        @Body request: ShareRequest
    ): BaseResponse<Unit>

    private companion object {
        private const val PATH_RESOURCE_ID = "resourceId"
        private const val SHARE = "share"
        private const val SIMULATE_RESOURCE_SHARE = "$SHARE/simulate/resource/{$PATH_RESOURCE_ID}.json"
        private const val SHARE_RESOURCE = "$SHARE/resource/{$PATH_RESOURCE_ID}.json"
    }
}
