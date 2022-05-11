package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.PermissionWithGroupDto
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission

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
    private val groupsModelMapper: GroupsModelMapper
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

    fun map(permission: ResourcePermission) = when (permission) {
        ResourcePermission.READ -> Permission.READ
        ResourcePermission.UPDATE -> Permission.WRITE
        ResourcePermission.OWNER -> Permission.OWNER
    }

    fun map(permissionWithGroups: PermissionWithGroupDto): PermissionModel =
        if (permissionWithGroups.group != null) {
            PermissionModel.GroupPermissionModel(
                map(permissionWithGroups.type),
                groupsModelMapper.map(permissionWithGroups.group!!)
            )
        } else {
            PermissionModel.UserPermissionModel(
                map(permissionWithGroups.type),
                permissionWithGroups.aroForeignKey!!
            )
        }
}
