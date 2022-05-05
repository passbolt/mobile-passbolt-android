package com.passbolt.mobile.android.database.impl.folders

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderWithChildItemsCount

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
interface FoldersDao : BaseDao<Folder> {

    @Transaction
    @Query(
        "SELECT folderId, name, permission, parentId, isShared, " +
                "(SELECT " +
                "( " +
                "(SELECT count(*) FROM Folder f_count WHERE f_count.parentId is f.folderId)" +
                " + " +
                "(SELECT count(*) FROM Resource r_count WHERE r_count.folderId is f.folderId)" +
                ")" +
                ") AS childItemsCount " +
                "FROM Folder f"
    )
    suspend fun getAllFolders(): List<FolderWithChildItemsCount>

    @Transaction
    @Query(
        "SELECT folderId, name, permission, parentId, isShared, " +
                "(SELECT " +
                "( " +
                "(SELECT count(*) FROM folder f1 WHERE f1.parentId is k.folderId)" +
                " + " +
                "(SELECT count(*) FROM resource r1 WHERE r1.folderId is k.folderId)" +
                ")" +
                ") AS childItemsCount " +
                "FROM folder k " +
                "WHERE k.parentId IS :folderId"
    )
    suspend fun getFolderDirectChildFolders(folderId: String?): List<FolderWithChildItemsCount>

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
                "SELECT folderId, name, permission, parentId, isShared, " +
                "(SELECT" +
                "(" +
                "(select count(*) from  folder fc where fc.parentId is a.folderId) + " +
                "(select count(*) from resource rc where rc.folderId is a.folderId) " +
                ")" +
                ") AS childItemsCount " +
                "" +
                "FROM ancestor a " +
                "WHERE level > 0 " +
                "ORDER BY level"
    )
    suspend fun getFolderAllChildFoldersRecursively(folderId: String): List<FolderWithChildItemsCount>

    @Transaction
    @Query("DELETE FROM Folder")
    suspend fun deleteAll()
}
