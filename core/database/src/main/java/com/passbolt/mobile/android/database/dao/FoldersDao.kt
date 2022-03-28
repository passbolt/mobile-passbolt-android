package com.passbolt.mobile.android.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.entity.resource.Folder
import com.passbolt.mobile.android.entity.resource.FolderWithChildResourcesAndChildFolders
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
interface FoldersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folderEntities: List<Folder>)

    @Transaction
    @Query("DELETE FROM Folder")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM Folder WHERE folderId=:id")
    suspend fun getFoldersWithResourcesForFolderWithId(id: String): FolderWithChildResourcesAndChildFolders

    @Transaction
    @Query("SELECT * FROM Folder WHERE parentId IS NULL")
    suspend fun getFoldersForRootFolder(): List<Folder>

    @Transaction
    @Query("SELECT * FROM Resource WHERE folderId IS NULL")
    suspend fun getResourcesForRootFolder(): List<Resource>

    @Transaction
    @Query(
        "SELECT " +
                "(SELECT COUNT(*) FROM Resource WHERE folderId IS :id) + " +
                "(SELECT COUNT(*) FROM Folder WHERE parentId IS :id)"
    )
    suspend fun getResourcesAndFoldersCountForFolderWithId(id: String): Int

    @Transaction
    @Query(
        "WITH RECURSIVE ancestor(folderId, name, permission, parentId, isShared, level) as (" +
                "SELECT folderId, name, permission, parentId, isShared, 0 " +
                "from Folder " +
                "WHERE folderId = :folderId " +
                "" +
                "UNION ALL " +
                "" +
                "SELECT f.folderId, f.name, f.permission, f.parentId, f.isShared, a.level + 1 " +
                "FROM Folder f " +
                "JOIN ancestor a on f.parentId = a.folderId " +
                ") " +
                "" +
                "SELECT folderId, name, permission, parentId, isShared " +
                "FROM ancestor " +
                "WHERE level > 1 " +
                "ORDER BY level"
    )
    suspend fun getFilteredSubFoldersRecursivelyForFolderWithId(
        folderId: String
    ): List<Folder>

    @Transaction
    @Query("SELECT * FROM Folder")
    suspend fun getAllFolders(): List<Folder>
}
