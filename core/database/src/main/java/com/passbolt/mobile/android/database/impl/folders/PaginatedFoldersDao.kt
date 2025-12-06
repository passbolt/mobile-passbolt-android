package com.passbolt.mobile.android.database.impl.folders

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderWithChildItemsCountAndPath

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
interface PaginatedFoldersDao : BaseDao<Folder> {
    @Transaction
    @Query(
        "SELECT folderId, name, permission, parentId, isShared, " +
            "(SELECT  " +
            "   (  " +
            "       (SELECT count(*) FROM folder f1 WHERE f1.parentId is k.folderId) " +
            " + " +
            "       (SELECT count(*) FROM resource r1 WHERE r1.folderId is k.folderId) " +
            "   ) " +
            ") AS childItemsCount," +
            "(" +
            "WITH RECURSIVE ancestor(folderId, name, parentId, level) as ( " +
            "   SELECT folderId, name, parentId, 0  " +
            "   FROM Folder  " +
            "   WHERE folderId = :folderId" +
            "" +
            "   UNION ALL  " +
            "" +
            "   SELECT f.folderId, f.name, f.parentId, a.level - 1  " +
            "   FROM Folder f  " +
            "   JOIN ancestor a on f.folderId = a.parentId  " +
            ") " +
            "SELECT GROUP_CONCAT(name, ' › ')" +
            "FROM " +
            "   (SELECT name FROM ancestor a order by a.level)" +
            ") as path " +
            "FROM folder k  " +
            "WHERE k.parentId IS :folderId " +
            "AND ( " +
            "   :searchQuery IS NULL OR " +
            "   k.name LIKE '%' || :searchQuery || '%' " +
            ")",
    )
    fun getFolderDirectChildFolders(
        folderId: String?,
        searchQuery: String?,
    ): PagingSource<Int, FolderWithChildItemsCountAndPath>

    @Transaction
    @Query(
        "SELECT folderId, name, permission, parentId, isShared, " +
            "(SELECT " +
            "( " +
            "(SELECT count(*) FROM Folder f_count WHERE f_count.parentId is f.folderId)" +
            " + " +
            "(SELECT count(*) FROM Resource r_count WHERE r_count.folderId is f.folderId)" +
            ")" +
            ") AS childItemsCount, " +
            "(" +
            "WITH RECURSIVE ancestor(folderId, name, parentId, level) as ( " +
            "   SELECT folderId, name, parentId, 0  " +
            "   FROM Folder  " +
            "   WHERE folderId = f.folderId" +
            "" +
            "   UNION ALL  " +
            "" +
            "   SELECT f.folderId, f.name, f.parentId, a.level - 1  " +
            "   FROM Folder f  " +
            "   JOIN ancestor a on f.folderId = a.parentId  " +
            ") " +
            "SELECT GROUP_CONCAT(name, ' › ')" +
            "FROM " +
            "   (SELECT name FROM ancestor a order by a.level)" +
            ") as path " +
            "FROM Folder f " +
            "WHERE :searchQuery IS NULL OR " +
            "f.name LIKE '%' || :searchQuery || '%'",
    )
    fun getAllFolders(searchQuery: String?): PagingSource<Int, FolderWithChildItemsCountAndPath>

    @Transaction
    @Query(
        "WITH RECURSIVE ancestor(folderId, name, permission, parentId, isShared, level) as (" +
            "SELECT folderId, name, permission, parentId, isShared, 0 " +
            "from Folder " +
            "WHERE folderId IS :folderId " +
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
            ") AS childItemsCount, " +
            "(" +
            "WITH RECURSIVE ancestor_path(folderId, name, parentId, level) as ( " +
            "   SELECT folderId, name, parentId, 0  " +
            "   FROM Folder  " +
            "   WHERE folderId = a.folderId" +
            "" +
            "   UNION ALL  " +
            "" +
            "   SELECT f.folderId, f.name, f.parentId, ap.level - 1  " +
            "   FROM Folder f  " +
            "   JOIN ancestor_path ap on f.folderId = ap.parentId  " +
            ") " +
            "SELECT GROUP_CONCAT(name, ' › ')" +
            "FROM " +
            "   (SELECT name FROM ancestor_path order by ancestor_path.level)" +
            ") as path " +
            "" +
            "FROM ancestor a " +
            "WHERE level > 0 " +
            "AND (:searchQuery IS NULL OR " +
            "a.name LIKE '%' || :searchQuery || '%') " +
            "ORDER BY level",
    )
    fun getFolderAllChildFoldersRecursively(
        folderId: String,
        searchQuery: String?,
    ): PagingSource<Int, FolderWithChildItemsCountAndPath>
}
