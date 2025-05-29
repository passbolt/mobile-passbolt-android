package com.passbolt.mobile.android.passboltapi.resource

import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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

internal interface ResourceApi {
    @GET(RESOURCES)
    suspend fun getResources(
        // always return index with current user permission
        @Query(QUERY_CONTAIN_PERMISSION) containingPermissions: Int? = 1,
        // always return index with favourite info
        @Query(QUERY_CONTAIN_FAVOURITE) containingFavourite: Int? = 1,
        // always return index with tag info
        @Query(QUERY_CONTAIN_TAG) containingTag: Int? = 1,
        // always return index with all permissions
        @Query(QUERY_CONTAIN_PERMISSIONS) containingGroup: Int? = 1,
    ): BaseResponse<List<ResourceResponseDto>>

    @DELETE(RESOURCE_BY_ID)
    suspend fun deleteResource(
        @Path(PATH_RESOURCE_ID) resourceId: String,
    ): BaseResponse<String?>

    @POST(RESOURCES)
    suspend fun createResource(
        @Body createResourceDto: CreateResourceDto,
        // always return index with permissions
        @Query(QUERY_CONTAIN_PERMISSION) containingPermissions: Int? = 1,
    ): BaseResponse<ResourceResponseDto>

    @PUT(RESOURCE_BY_ID)
    suspend fun updateResource(
        @Path(PATH_RESOURCE_ID) resourceId: String,
        @Body createResourceDto: CreateResourceDto,
    ): BaseResponse<ResourceResponseDto>

    private companion object {
        private const val PATH_RESOURCE_ID = "resourceId"
        private const val PATH_RESOURCES = "resources"
        private const val QUERY_CONTAIN_PERMISSION = "contain[permission]"
        private const val QUERY_CONTAIN_FAVOURITE = "contain[favorite]"
        private const val QUERY_CONTAIN_TAG = "contain[tag]"
        private const val QUERY_CONTAIN_PERMISSIONS = "contain[permissions.group]"
        private const val RESOURCES = "resources.json"
        private const val RESOURCE_BY_ID = "$PATH_RESOURCES/{$PATH_RESOURCE_ID}.json"
    }
}
