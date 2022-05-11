package com.passbolt.mobile.android.entity.group

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.passbolt.mobile.android.entity.resource.Permission

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
@Entity
data class UsersGroup(
    @PrimaryKey
    val groupId: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String
)

data class UsersGroupWithChildItemsCount(
    val groupId: String,
    val name: String,
    val childItemsCount: Int
)

@Entity(primaryKeys = ["resourceId", "groupId"])
data class ResourceAndGroupsCrossRef(
    val resourceId: String,
    val groupId: String,
    val permission: Permission
)

@Entity(primaryKeys = ["userId", "groupId"])
data class UsersAndGroupCrossRef(
    val userId: String,
    val groupId: String
)
