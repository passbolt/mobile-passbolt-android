package com.passbolt.mobile.android.database.impl.resources

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.permission.GroupPermission
import com.passbolt.mobile.android.entity.permission.UserPermission
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
interface ResourcesDao : BaseDao<Resource> {

    @Transaction
    @Query(
        "SELECT * FROM Resource " +
                "WHERE resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ") " +
                "ORDER BY resourceName " +
                "COLLATE NOCASE ASC"
    )
    suspend fun getAllOrderedByName(slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource " +
                "WHERE favouriteId IS NOT NULL AND resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ") " +
                "ORDER BY modified DESC"
    )
    suspend fun getFavourites(slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource " +
                "WHERE resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ") " +
                "ORDER BY modified DESC"
    )
    suspend fun getAllOrderedByModifiedDate(slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource " +
                "WHERE resourcePermission IN (:permissions) AND resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ")" +
                "ORDER BY modified DESC"
    )
    suspend fun getWithPermissions(permissions: Set<Permission>, slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource WHERE (" +
                "resourceName LIKE '%' || :searchQuery || '%' OR " +
                "url LIKE '%' || :searchQuery || '%' OR " +
                "username LIKE '%' || :searchQuery || '%') " +
                "AND " +
                "folderId IN (:inOneOfFolders) " +
                "AND " +
                "resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ")" +
                "ORDER BY modified DESC"
    )
    suspend fun getFilteredForChildFolders(
        searchQuery: String,
        inOneOfFolders: List<String>,
        slugs: List<String>
    ): List<Resource>

    @Transaction
    @Query("SELECT * FROM Resource WHERE resourceId == :resourceId")
    suspend fun get(resourceId: String): Resource

    @Transaction
    @Query(
        "SELECT * FROM Resource " +
                "WHERE folderId IS :folderId AND resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ")"
    )
    suspend fun getResourcesForFolderWithId(folderId: String?, slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource r " +
                "INNER JOIN ResourceAndTagsCrossRef cr " +
                "ON r.resourceId=cr.resourceId " +
                "WHERE cr.tagId=:tagId AND r.resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ")"
    )
    suspend fun getResourcesWithTag(tagId: String, slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource r " +
                "INNER JOIN ResourceAndGroupsCrossRef cr " +
                "ON r.resourceId=cr.resourceId " +
                "WHERE cr.groupId=:groupId AND r.resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ")"
    )
    suspend fun getResourcesWithGroup(groupId: String, slugs: List<String>): List<Resource>

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
    suspend fun getResourceUsersPermissions(resourceId: String): List<UserPermission>

    @Transaction
    @Query(
        "SELECT rGCR.groupId, rGCR.permission, rGCR.permissionId ,ug.name as groupName from Resource r " +
                "INNER JOIN ResourceAndGroupsCrossRef rGCR " +
                "ON rGCR.resourceId = r.resourceId " +
                "INNER JOIN UsersGroup ug " +
                "ON ug.groupId = rGCR.groupId " +
                "where r.resourceId = :resourceId"
    )
    suspend fun getResourceGroupsPermissions(resourceId: String): List<GroupPermission>

    @Transaction
    @Query(
        "SELECT * FROM Resource r " +
                "INNER JOIN ResourceAndTagsCrossRef rTCR " +
                "ON rTCR.resourceId = r.resourceId " +
                "INNER JOIN Tag t " +
                "ON t.id =rTCr.tagId " +
                "WHERE (t.slug LIKE '%' || :tagSearchQuery || '%') AND r.resourceTypeId IN(" +
                "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
                ")" +
                "GROUP BY r.resourceId " +
                "ORDER BY resourceName COLLATE NOCASE ASC "
    )
    suspend fun getAllThatHaveTagContaining(tagSearchQuery: String, slugs: List<String>): List<Resource>

    @Transaction
    @Query(
        "SELECT * FROM Resource " +
                "WHERE resourceTypeId IN(" +
                "SELECT resourceTypeId FROM ResourceType WHERE slug IN (:otpSlugs)" +
                ")" +
                "ORDER BY resourceName " +
                "COLLATE NOCASE ASC"
    )
    suspend fun getAllOtpResources(otpSlugs: List<String> = otpResourceTypeSlugs): List<Resource>

    @Transaction
    @Query("DELETE FROM Resource")
    suspend fun deleteAll()

    private companion object {
        private val otpResourceTypeSlugs = listOf(
            "totp", "password-description-totp"
        )
    }
}
