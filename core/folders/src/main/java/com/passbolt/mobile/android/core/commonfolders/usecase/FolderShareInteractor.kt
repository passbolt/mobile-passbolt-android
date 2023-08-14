package com.passbolt.mobile.android.core.commonfolders.usecase

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.ui.PermissionModelUi
import retrofit2.HttpException
import java.net.HttpURLConnection

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
class FolderShareInteractor(
    private val shareFolderUseCase: ShareFolderUseCase,
    private val sharePermissionsModelMapper: SharePermissionsModelMapper,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase
) {

    suspend fun shareFolder(folderId: String, permissions: List<PermissionModelUi>): Output {
        val existingPermissions =
            getLocalFolderPermissionsUseCase.execute(GetLocalFolderPermissionsUseCase.Input(folderId))
                .permissions

        val sharePermissions = sharePermissionsModelMapper.mapForShare(
            SharePermissionsModelMapper.ShareItem.Folder(folderId), permissions, existingPermissions
        )

        return when (val output = shareFolderUseCase.execute(ShareFolderUseCase.Input(folderId, sharePermissions))) {
            is ShareFolderUseCase.Output.Failure -> Output.ShareFailure(output.result.exception)
            is ShareFolderUseCase.Output.Success -> Output.Success
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = if (
                (this is ShareFailure &&
                        (this.exception as? HttpException)?.code() == HttpURLConnection.HTTP_UNAUTHORIZED)
            ) {
                AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
            } else if (this is Unauthorized) {
                AuthenticationState.Unauthenticated(this.reason)
            } else {
                AuthenticationState.Authenticated
            }

        data class ShareFailure(val exception: Exception) : Output()

        class Unauthorized(val reason: UnauthenticatedReason) : Output()

        object Success : Output()
    }
}
