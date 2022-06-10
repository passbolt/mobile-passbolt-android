package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.PermissionWithGroupDto
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.ResourceGroupPermission
import com.passbolt.mobile.android.entity.resource.ResourceUserPermission
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserWithAvatar

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

class PermissionsModelMapper(
    private val groupsModelMapper: GroupsModelMapper,
    private val usersModelMapper: UsersModelMapper
) {

    fun map(permission: Permission) = when (permission) {
        Permission.READ -> ResourcePermission.READ
        Permission.WRITE -> ResourcePermission.UPDATE
        Permission.OWNER -> ResourcePermission.OWNER
    }

    @Suppress("MagicNumber")
    fun map(permissionInt: Int) = when (permissionInt) {
        1 -> ResourcePermission.READ
        7 -> ResourcePermission.UPDATE
        15 -> ResourcePermission.OWNER
        else -> throw IllegalArgumentException("Unsupported DTO permission value: $permissionInt")
    }

    @Suppress("MagicNumber")
    fun mapToPermissionInt(permission: ResourcePermission) = when (permission) {
        ResourcePermission.READ -> 1
        ResourcePermission.UPDATE -> 7
        ResourcePermission.OWNER -> 15
    }

    fun map(permission: ResourcePermission) = when (permission) {
        ResourcePermission.READ -> Permission.READ
        ResourcePermission.UPDATE -> Permission.WRITE
        ResourcePermission.OWNER -> Permission.OWNER
    }

    fun map(permissionWithGroups: PermissionWithGroupDto): PermissionModel =
        if (permissionWithGroups.group != null) {
            PermissionModel.GroupPermissionModel(
                map(permissionWithGroups.type),
                permissionWithGroups.id,
                groupsModelMapper.map(permissionWithGroups.group!!)
            )
        } else {
            PermissionModel.UserPermissionModel(
                map(permissionWithGroups.type),
                permissionWithGroups.id,
                permissionWithGroups.aroForeignKey!!
            )
        }

    fun map(
        groupsPermissions: List<ResourceGroupPermission>,
        usersPermissions: List<ResourceUserPermission>
    ) =
        mutableListOf<PermissionModelUi>()
            .apply {
                groupsPermissions.mapTo(this) {
                    PermissionModelUi.GroupPermissionModel(
                        map(it.permission),
                        it.permissionId,
                        groupsModelMapper.map(it)
                    )
                }
                usersPermissions.mapTo(this) {
                    PermissionModelUi.UserPermissionModel(
                        map(it.permission),
                        it.permissionId,
                        UserWithAvatar(
                            it.userId,
                            it.firstName.orEmpty(),
                            it.lastName.orEmpty(),
                            it.userName,
                            it.avatarUrl
                        )
                    )
                }
            }

    fun map(model: GroupModel, permission: ResourcePermission, permissionId: String) =
        PermissionModelUi.GroupPermissionModel(permission, permissionId, model)

    fun map(model: UserModel, permission: ResourcePermission, permissionId: String) =
        PermissionModelUi.UserPermissionModel(permission, permissionId, usersModelMapper.mapToUserWithAvatar(model))
}
