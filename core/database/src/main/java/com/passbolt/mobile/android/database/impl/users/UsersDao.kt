package com.passbolt.mobile.android.database.impl.users

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.user.User
import com.passbolt.mobile.android.entity.user.UserUpdateState

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
interface UsersDao : BaseDao<User> {
    @Transaction
    @Query("SELECT * FROM User WHERE id=:userId")
    suspend fun get(userId: String): User

    @Transaction
    @Query(
        "SELECT * FROM User u " +
            "WHERE :ftsQuery IS NULL OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM UserFts WHERE UserFts MATCH :ftsQuery AND UserFts.docid = u.rowid" +
            "   ) " +
            "ORDER BY u.firstName COLLATE NOCASE ASC, u.lastName COLLATE NOCASE ASC, u.userName COLLATE NOCASE ASC",
    )
    suspend fun getAll(ftsQuery: String?): List<User>

    @Query("UPDATE User SET updateState = :updateState")
    suspend fun setAllUpdateState(updateState: UserUpdateState)

    @Query("DELETE FROM User WHERE updateState = :updateState")
    suspend fun removeWithUpdateState(updateState: UserUpdateState)
}
