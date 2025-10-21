package com.passbolt.mobile.android.database.impl.resources

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.permission.GroupPermission
import com.passbolt.mobile.android.entity.permission.UserPermission
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceWithMetadata
import java.time.ZonedDateTime

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
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId ",
    )
    suspend fun getAll(): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef rTCR " +
            "ON r.resourceId = rTCR.resourceId " +
            "LEFT JOIN Tag t " +
            "ON t.id = rTCR.tagId " +
            "WHERE r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%' OR " +
            "   t.slug LIKE '%' || :searchQuery || '%'" +
            ") " +
            "ORDER BY rm.name " +
            "COLLATE NOCASE ASC",
    )
    suspend fun getAllOrderedByName(
        slugs: Set<String>,
        searchQuery: String?,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef rTCR " +
            "ON r.resourceId = rTCR.resourceId " +
            "LEFT JOIN Tag t " +
            "ON t.id = rTCR.tagId " +
            "WHERE r.favouriteId IS NOT NULL AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%' OR " +
            "   t.slug LIKE '%' || :searchQuery || '%'" +
            ") " +
            "ORDER BY modified DESC",
    )
    suspend fun getFavourites(
        slugs: Set<String>,
        searchQuery: String?,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT DISTINCT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef rTCR " +
            "ON r.resourceId = rTCR.resourceId " +
            "LEFT JOIN Tag t " +
            "ON t.id = rTCR.tagId " +
            "WHERE r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%' OR " +
            "   t.slug LIKE '%' || :searchQuery || '%'" +
            ") " +
            "GROUP BY r.resourceId " +
            "ORDER BY r.modified DESC",
    )
    suspend fun getAllOrderedByModifiedDate(
        slugs: Set<String>,
        searchQuery: String?,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef rTCR " +
            "ON r.resourceId = rTCR.resourceId " +
            "LEFT JOIN Tag t " +
            "ON t.id = rTCR.tagId " +
            "WHERE r.resourcePermission IN (:permissions) AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ")" +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%' OR " +
            "   t.slug LIKE '%' || :searchQuery || '%'" +
            ") " +
            "ORDER BY modified DESC",
    )
    suspend fun getWithPermissions(
        permissions: Set<Permission>,
        slugs: Set<String>,
        searchQuery: String?,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.resourceId == :resourceId",
    )
    suspend fun get(resourceId: String): ResourceWithMetadata

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson, ru.uri " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef rTCR " +
            "ON r.resourceId = rTCR.resourceId " +
            "LEFT JOIN Tag t " +
            "ON t.id = rTCR.tagId " +
            "WHERE (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%' OR " +
            "   t.slug LIKE '%' || :searchQuery || '%' )" +
            "AND " +
            "r.folderId IN (:inOneOfFolders) " +
            "AND " +
            "r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ")" +
            "ORDER BY modified DESC",
    )
    suspend fun getFilteredForChildFolders(
        searchQuery: String,
        inOneOfFolders: List<String>,
        slugs: Set<String>,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.folderId IS :folderId AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ")",
    )
    suspend fun getResourcesForFolderWithId(
        folderId: String?,
        slugs: Set<String>,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef cr " +
            "ON r.resourceId=cr.resourceId " +
            "WHERE cr.tagId=:tagId AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%'" +
            ") ",
    )
    suspend fun getResourcesWithTag(
        tagId: String,
        slugs: Set<String>,
        searchQuery: String?,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "INNER JOIN ResourceAndGroupsCrossRef cr " +
            "ON r.resourceId=cr.resourceId " +
            "WHERE cr.groupId=:groupId AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%'" +
            ") ",
    )
    suspend fun getResourcesWithGroup(
        groupId: String,
        slugs: Set<String>,
        searchQuery: String?,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT rUCR.userId, rUCR.permission, rUCR.permissionId, " +
            "u.firstName, u.lastName, u.avatarUrl, u.userName, u.fingerprint, u.disabled " +
            "FROM Resource r " +
            "INNER JOIN ResourceAndUsersCrossRef rUCR " +
            "ON rUCR.resourceId = r.resourceId " +
            "INNER JOIN User u " +
            "ON u.id = rUCR.userId " +
            "WHERE r.resourceId = :resourceId",
    )
    suspend fun getResourceUsersPermissions(resourceId: String): List<UserPermission>

    @Transaction
    @Query(
        "SELECT rGCR.groupId, rGCR.permission, rGCR.permissionId ,ug.name as groupName from Resource r " +
            "INNER JOIN ResourceAndGroupsCrossRef rGCR " +
            "ON rGCR.resourceId = r.resourceId " +
            "INNER JOIN UsersGroup ug " +
            "ON ug.groupId = rGCR.groupId " +
            "where r.resourceId = :resourceId",
    )
    suspend fun getResourceGroupsPermissions(resourceId: String): List<GroupPermission>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "INNER JOIN ResourceAndTagsCrossRef rTCR " +
            "ON rTCR.resourceId = r.resourceId " +
            "INNER JOIN Tag t " +
            "ON t.id =rTCr.tagId " +
            "WHERE (t.slug LIKE '%' || :tagSearchQuery || '%') AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ")" +
            "GROUP BY r.resourceId " +
            "ORDER BY rm.name COLLATE NOCASE ASC ",
    )
    suspend fun getAllThatHaveTagContaining(
        tagSearchQuery: String,
        slugs: Set<String>,
    ): List<ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "LEFT JOIN ResourceUri ru " +
            "ON r.resourceId = ru.resourceId " +
            "LEFT JOIN ResourceAndTagsCrossRef rTCR " +
            "ON r.resourceId = rTCR.resourceId " +
            "LEFT JOIN Tag t " +
            "ON t.id = rTCR.tagId " +
            "WHERE r.expiry IS NOT NULL AND r.expiry < :expiryTimestampMillis AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :searchQuery IS NULL OR " +
            "   rm.name LIKE '%' || :searchQuery || '%' OR " +
            "   rm.username LIKE '%' || :searchQuery || '%' OR " +
            "   rm.customFieldsKeys LIKE '%' || :searchQuery || '%' OR " +
            "   ru.uri LIKE '%' || :searchQuery || '%' OR " +
            "   t.slug LIKE '%' || :searchQuery || '%'" +
            ") " +
            "ORDER BY expiry ASC",
    )
    suspend fun getExpiredResources(
        slugs: Set<String>,
        searchQuery: String?,
        expiryTimestampMillis: Long = ZonedDateTime.now().toInstant().toEpochMilli(),
    ): List<ResourceWithMetadata>

    @Transaction
    @Query("DELETE FROM Resource")
    suspend fun deleteAll()
}
