package com.passbolt.mobile.android.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource

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
interface ResourcesDao {

    @Transaction
    @Query("SELECT * FROM Resource ORDER BY resourceName COLLATE NOCASE ASC")
    suspend fun getAllOrderedByName(): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE isFavourite==1 ORDER BY resourceName COLLATE NOCASE ASC")
    suspend fun getFavourites(): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource ORDER BY modified DESC")
    suspend fun getAllOrderedByModifiedDate(): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE resourcePermission IN (:permissions)")
    suspend fun getWithPermissions(permissions: Set<Permission>): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE resourceId == :resourceId")
    suspend fun get(resourceId: String): Resource

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resourceEntities: List<Resource>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resourceEntities: Resource)

    @Update
    suspend fun update(resourceEntities: Resource)

    @Transaction
    @Query("DELETE FROM Resource")
    suspend fun deleteAll()
}
