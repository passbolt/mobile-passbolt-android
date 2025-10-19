package com.passbolt.mobile.android.passboltapi.resource

import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.response.BasePaginatedResponse
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.ResourceResponseDto

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
internal class ResourceRemoteDataSource(
    private val resourceApi: ResourceApi,
) : ResourceDataSource {
    override suspend fun getResources(): BaseResponse<List<ResourceResponseDto>> = resourceApi.getResources()

    override suspend fun getResourcesPaginated(
        limit: Int,
        page: Int,
    ): BasePaginatedResponse<List<ResourceResponseDto>> =
        resourceApi.getResourcesPaginated(
            limit = limit,
            page = page,
        )

    override suspend fun createResource(createResourceDto: CreateResourceDto) = resourceApi.createResource(createResourceDto)

    override suspend fun deleteResource(resourceId: String) = resourceApi.deleteResource(resourceId)

    override suspend fun updateResource(
        resourceId: String,
        createResourceDto: CreateResourceDto,
    ) = resourceApi.updateResource(resourceId, createResourceDto)
}
