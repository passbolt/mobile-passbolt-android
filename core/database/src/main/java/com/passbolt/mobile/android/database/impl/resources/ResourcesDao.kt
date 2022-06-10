package com.passbolt.mobile.android.database.impl.resources

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceGroupPermission
import com.passbolt.mobile.android.entity.resource.ResourceUserPermission

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
interface ResourcesDao : BaseDao<Resource> {

    @Transaction
    @Query("SELECT * FROM Resource ORDER BY resourceName COLLATE NOCASE ASC")
    suspend fun getAllOrderedByName(): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE isFavourite==1 ORDER BY modified DESC")
    suspend fun getFavourites(): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource ORDER BY modified DESC")
    suspend fun getAllOrderedByModifiedDate(): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE resourcePermission IN (:permissions) ORDER BY modified DESC")
    suspend fun getWithPermissions(permissions: Set<Permission>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource WHERE (" +
                "resourceName LIKE '%' || :searchQuery || '%' OR " +
                "url LIKE '%' || :searchQuery || '%' OR " +
                "username LIKE '%' || :searchQuery || '%') " +
                "AND " +
                "folderId IN (:inOneOfFolders) " +
                "ORDER BY modified DESC"
    )
    suspend fun getFilteredForChildFolders(searchQuery: String, inOneOfFolders: List<String>): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE resourceId == :resourceId")
    suspend fun get(resourceId: String): Resource

    @Transaction
    @Query("SELECT * FROM Resource WHERE folderId IS :folderId")
    suspend fun getResourcesForFolderWithId(folderId: String?): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource r " +
                "INNER JOIN ResourceAndTagsCrossRef cr " +
                "ON r.resourceId=cr.resourceId " +
                "WHERE cr.tagId=:tagId"
    )
    suspend fun getResourcesWithTag(tagId: String): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource r " +
                "INNER JOIN ResourceAndGroupsCrossRef cr " +
                "ON r.resourceId=cr.resourceId " +
                "WHERE cr.groupId=:groupId"
    )
    suspend fun getResourcesWithGroup(groupId: String): List<Resource>

    @Transaction
    @Query(
        "SELECT rUCR.userId, rUCR.permission, rUCR.permissionId, " +
                "u.firstName, u.lastName, u.avatarUrl, u.userName, u.fingerprint " +
                "FROM Resource r " +
                "INNER JOIN ResourceAndUsersCrossRef rUCR " +
                "ON rUCR.resourceId = r.resourceId " +
                "INNER JOIN User u " +
                "ON u.id = rUCR.userId " +
                "WHERE r.resourceId = :resourceId"
    )
    suspend fun getResourceUsersPermissions(resourceId: String): List<ResourceUserPermission>

    @Transaction
    @Query(
        "SELECT rGCR.groupId, rGCR.permission, rGCR.permissionId ,ug.name as groupName from Resource r " +
                "INNER JOIN ResourceAndGroupsCrossRef rGCR " +
                "ON rGCR.resourceId = r.resourceId " +
                "INNER JOIN UsersGroup ug " +
                "ON ug.groupId = rGCR.groupId " +
                "where r.resourceId = :resourceId"
    )
    suspend fun getResourceGroupsPermissions(resourceId: String): List<ResourceGroupPermission>

    @Transaction
    @Query("DELETE FROM Resource")
    suspend fun deleteAll()
}
