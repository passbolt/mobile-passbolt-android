package com.passbolt.mobile.android.core.resources.usecase.db

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.entity.group.ResourceAndGroupsCrossRef
import com.passbolt.mobile.android.entity.user.ResourceAndUsersCrossRef
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes

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
class AddLocalResourcePermissionsUseCase(
    private val databaseProvider: DatabaseProvider,
    private val permissionsModelMapper: PermissionsModelMapper,
) : AsyncUseCase<AddLocalResourcePermissionsUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        val db = databaseProvider.get(selectedAccountId)
        val resourcesAndGroupsCrossRefDao = db.resourcesAndGroupsCrossRefDao()
        val resourcesAndUsersCrossRefDao = db.resourcesAndUsersCrossRefDao()

        input.resourcesWithTagsModelAndGroups.apply {
            val resourceGroupPermissions =
                map {
                    it.resourceModel.resourceId to
                        it.resourcePermissions
                            .filterIsInstance<PermissionModel.GroupPermissionModel>()
                }
            val resourceUserPermissions =
                map {
                    it.resourceModel.resourceId to
                        it.resourcePermissions
                            .filterIsInstance<PermissionModel.UserPermissionModel>()
                }

            val resourceAndGroupCrossRefs =
                resourceGroupPermissions
                    .flatMap { (resourceId, groupPermissions) ->
                        groupPermissions.map { groupPermission ->
                            ResourceAndGroupsCrossRef(
                                resourceId,
                                groupPermission.group.groupId,
                                permissionsModelMapper.map(groupPermission.permission),
                                groupPermission.permissionId,
                            )
                        }
                    }

            val resourceAndUsersCrossRefs =
                resourceUserPermissions
                    .flatMap { (resourceId, userPermissions) ->
                        userPermissions.map { userPermission ->
                            ResourceAndUsersCrossRef(
                                resourceId,
                                userPermission.userId,
                                permissionsModelMapper.map(userPermission.permission),
                                userPermission.permissionId,
                            )
                        }
                    }

            resourcesAndGroupsCrossRefDao.insertAll(resourceAndGroupCrossRefs)
            resourcesAndUsersCrossRefDao.insertAll(resourceAndUsersCrossRefs)
        }
    }

    data class Input(
        val resourcesWithTagsModelAndGroups: List<ResourceModelWithAttributes>,
    )
}
