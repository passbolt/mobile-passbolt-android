package com.passbolt.mobile.android.passboltapi.favourites

import com.passbolt.mobile.android.dto.response.AddToFavouritesResponseDto
import com.passbolt.mobile.android.dto.response.BaseResponse
import retrofit2.http.DELETE
import retrofit2.http.POST
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

internal interface FavouritesApi {

    @POST(FAVOURITE_RESOURCE_BY_ID)
    suspend fun addToFavourites(
        @Path(PATH_RESOURCE_ID) resourceId: String
    ): BaseResponse<AddToFavouritesResponseDto>

    @DELETE(FAVOURITES_BY_ID)
    suspend fun removeFromFavourites(
        @Path(PATH_FAVOURITE_ID) favouriteId: String
    ): BaseResponse<Unit>

    private companion object {
        private const val PATH_RESOURCE_ID = "resourceId"
        private const val PATH_FAVOURITE_ID = "favouriteId"
        private const val FAVOURITES = "favorites"
        private const val FAVOURITES_BY_ID = "$FAVOURITES/{$PATH_FAVOURITE_ID}.json"
        private const val FAVOURITE_RESOURCE_BY_ID = "$FAVOURITES/resource/{$PATH_RESOURCE_ID}.json"
    }
}
