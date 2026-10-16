package com.passbolt.mobile.android.database.impl.groups

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.group.GroupUpdateState
import com.passbolt.mobile.android.entity.group.GroupWithUsers
import com.passbolt.mobile.android.entity.group.UsersGroup
import com.passbolt.mobile.android.entity.group.UsersGroupWithChildItemsCount

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

@Dao
interface GroupsDao : BaseDao<UsersGroup> {
    @Transaction
    @Query(
        "SELECT * FROM UsersGroup g " +
            "WHERE :ftsQuery IS NULL OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM UsersGroupFts WHERE UsersGroupFts MATCH :ftsQuery AND UsersGroupFts.docid = g.rowid" +
            "   ) " +
            "ORDER BY name ASC",
    )
    suspend fun getAll(ftsQuery: String?): List<UsersGroup>

    @Query("UPDATE UsersGroup SET updateState = :updateState")
    suspend fun setAllUpdateState(updateState: GroupUpdateState)

    @Query("DELETE FROM UsersGroup WHERE updateState = :updateState")
    suspend fun removeWithUpdateState(updateState: GroupUpdateState)

    @Transaction
    @Query(
        "SELECT groupId, name, " +
            "(SELECT" +
            "(" +
            "(select distinct count(resourceId) " +
            "from resourceandgroupscrossref rGCR " +
            "where rGCR.groupId is g.groupId) " +
            ")" +
            ") AS childItemsCount " +
            "FROM UsersGroup g " +
            "WHERE :ftsQuery IS NULL OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM UsersGroupFts WHERE UsersGroupFts MATCH :ftsQuery AND UsersGroupFts.docid = g.rowid" +
            "   ) " +
            "ORDER BY name ASC",
    )
    suspend fun getAllWithSharedItemsCount(ftsQuery: String?): List<UsersGroupWithChildItemsCount>

    @Transaction
    @Query("SELECT * FROM UsersGroup WHERE groupId=:groupId ORDER BY name ASC")
    suspend fun getGroupWithUsers(groupId: String): GroupWithUsers
}
