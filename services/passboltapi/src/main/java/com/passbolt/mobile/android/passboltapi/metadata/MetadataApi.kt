package com.passbolt.mobile.android.passboltapi.metadata

import com.passbolt.mobile.android.dto.request.EncryptedDataAndModifiedRequest
import com.passbolt.mobile.android.dto.request.EncryptedDataRequest
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.MetadataKeysResponseDto
import com.passbolt.mobile.android.dto.response.MetadataKeysSettingsResponseDto
import com.passbolt.mobile.android.dto.response.MetadataSessionKeyResponseDto
import com.passbolt.mobile.android.dto.response.MetadataTypesSettingsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

internal interface MetadataApi {

    @GET(METADATA_KEYS_ENDPOINT)
    suspend fun getMetadataKeys(
        @Query(QUERY_CONTAIN_PRIVATE_KEYS) containPrivateKeys: Int? = 1
    ): BaseResponse<List<MetadataKeysResponseDto>>

    @GET(METADATA_TYPES_SETTINGS)
    suspend fun getMetadataTypesSettings(): BaseResponse<MetadataTypesSettingsResponseDto>

    @GET(METADATA_KEYS_SETTINGS)
    suspend fun getMetadataKeysSettings(): BaseResponse<MetadataKeysSettingsResponseDto>

    @GET(METADATA_SESSION_KEYS_ENDPOINT)
    suspend fun getMetadataSessionKeys(): BaseResponse<List<MetadataSessionKeyResponseDto>>

    @POST(METADATA_SESSION_KEYS_ENDPOINT)
    suspend fun postMetadataSessionKeys(@Body request: EncryptedDataRequest): BaseResponse<Unit>

    @POST(METADATA_SESSION_KEYS_UPDATE_ENDPOINT)
    suspend fun updateMetadataSessionKeys(
        @Path(QUERY_UUID) uuid: String,
        @Body request: EncryptedDataAndModifiedRequest
    ): BaseResponse<Unit>

    private companion object {
        private const val QUERY_CONTAIN_PRIVATE_KEYS = "contain[metadata_private_keys]"

        private const val QUERY_UUID = "uuid"
        private const val METADATA = "metadata"
        private const val SESSION_KEYS = "session-keys"
        private const val METADATA_KEYS_ENDPOINT = "$METADATA/keys.json"
        private const val METADATA_SESSION_KEYS_ENDPOINT = "$METADATA/session-keys.json"
        private const val METADATA_SESSION_KEYS_UPDATE_ENDPOINT = "$METADATA/$SESSION_KEYS/{$QUERY_UUID}.json"
        private const val METADATA_TYPES = "$METADATA/types"
        private const val METADATA_KEYS = "$METADATA/keys"
        private const val METADATA_TYPES_SETTINGS = "$METADATA_TYPES/settings.json"
        private const val METADATA_KEYS_SETTINGS = "$METADATA_KEYS/settings.json"
    }
}
