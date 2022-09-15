package com.passbolt.mobile.android.database.impl.folders

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.PermissionModelUi

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
class GetLocalFolderPermissionsToCopyAsNewUseCase(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase
) : AsyncUseCase<GetLocalFolderPermissionsToCopyAsNewUseCase.Input,
        GetLocalFolderPermissionsToCopyAsNewUseCase.Output> {

    /**
     * Gets folder permissions which are to be copied and applied to a newly created resource in that folder.
     * Current user permissions needs to be filtered out to not be duplicated.
     * Permission IDs need to be replaced to a constant SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID
     */
    override suspend fun execute(input: Input): Output {
        val currentAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val currentAccountServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)

        val foldersDao = databaseProvider
            .get(currentAccount)
            .foldersDao()

        val groupsPermissions = foldersDao.getFolderGroupsPermissions(input.folderId)
            .map { it.copy(permissionId = SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID) }
        val usersPermissions = foldersDao.getFolderUsersPermissions(input.folderId)
            .filter { it.userId != currentAccountServerId }
            .map { it.copy(permissionId = SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID) }

        return Output(
            permissionsModelMapper.map(
                groupsPermissions,
                usersPermissions
            )
        )
    }

    data class Input(
        val folderId: String
    )

    data class Output(
        val permissions: List<PermissionModelUi>
    )
}
