package com.passbolt.mobile.android.passboltapi.folders

import com.passbolt.mobile.android.dto.request.CreateFolderRequestDto
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.CreateFolderResponseDto
import com.passbolt.mobile.android.dto.response.FolderResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

internal interface FoldersApi {
    @GET(FOLDERS)
    suspend fun getFolders(
        // always return index with current user permission
        @Query(QUERY_CONTAIN_PERMISSION) containingPermission: Int? = 1,
        // always return index with all users and groups
        @Query(QUERY_CONTAIN_PERMISSIONS) containingPermissions: Int? = 1,
    ): BaseResponse<List<FolderResponseDto>>

    @POST(FOLDERS)
    suspend fun createFolder(
        @Body createFolderRequestDto: CreateFolderRequestDto,
        // always return index with current user permission
        @Query(QUERY_CONTAIN_PERMISSION) containingPermission: Int? = 1,
    ): BaseResponse<CreateFolderResponseDto>

    private companion object {
        private const val FOLDERS = "folders.json"

        private const val QUERY_CONTAIN_PERMISSION = "contain[permission]"
        private const val QUERY_CONTAIN_PERMISSIONS = "contain[permissions.group]"
    }
}
