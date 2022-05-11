package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.GroupsResponseDto
import com.passbolt.mobile.android.dto.response.PermissionGroupDto
import com.passbolt.mobile.android.entity.group.UsersGroup
import com.passbolt.mobile.android.entity.group.UsersGroupWithChildItemsCount
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.GroupModelWithUsers
import com.passbolt.mobile.android.ui.GroupUser
import com.passbolt.mobile.android.ui.GroupWithCount

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
class GroupsModelMapper {

    fun map(group: GroupsResponseDto): GroupModelWithUsers =
        GroupModelWithUsers(
            GroupModel(
                groupId = group.id,
                groupName = group.name
            ),
            group.users?.map { GroupUser(it.id) }.orEmpty()
        )

    fun map(group: GroupModel): UsersGroup =
        UsersGroup(
            groupId = group.groupId,
            name = group.groupName
        )

    fun map(group: UsersGroupWithChildItemsCount) =
        GroupWithCount(
            groupId = group.groupId,
            groupName = group.name,
            groupItemsCount = group.childItemsCount
        )

    fun map(permission: PermissionGroupDto) =
        GroupModel(
            permission.id,
            permission.name
        )
}
