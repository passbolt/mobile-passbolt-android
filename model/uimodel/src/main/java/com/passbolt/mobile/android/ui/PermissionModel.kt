package com.passbolt.mobile.android.ui

import kotlinx.serialization.Serializable

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
sealed class PermissionModel(
    val permission: ResourcePermission,
    val permissionId: String,
) {
    class UserPermissionModel(
        permission: ResourcePermission,
        permissionId: String,
        val userId: String,
    ) : PermissionModel(permission, permissionId)

    class GroupPermissionModel(
        permission: ResourcePermission,
        permissionId: String,
        val group: GroupModel,
    ) : PermissionModel(permission, permissionId)
}

@Serializable
sealed class PermissionModelUi {
    abstract val permission: ResourcePermission
    abstract val permissionId: String

    @Serializable
    data class UserPermissionModel(
        override val permission: ResourcePermission,
        override val permissionId: String,
        val user: UserWithAvatar,
    ) : PermissionModelUi()

    @Serializable
    data class GroupPermissionModel(
        override val permission: ResourcePermission,
        override val permissionId: String,
        val group: GroupModel,
    ) : PermissionModelUi()
}

@Serializable
data class UserWithAvatar(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val isDisabled: Boolean,
    val avatarUrl: String?,
) {
    val fullName: String
        get() = "$firstName $lastName"
}
