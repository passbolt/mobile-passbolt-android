package com.passbolt.mobile.android.database.impl.resources

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.passbolt.mobile.android.database.impl.base.BaseDao
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
interface PaginatedResourcesDao : BaseDao<Resource> {
    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            ")) " +
            "ORDER BY rm.name " +
            "COLLATE NOCASE ASC",
    )
    fun getAllOrderedByNamePaginated(
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            ")) " +
            "ORDER BY r.modified DESC",
    )
    fun getAllOrderedByModifiedDatePaginated(
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.favouriteId IS NOT NULL AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            ")) " +
            "ORDER BY modified DESC",
    )
    fun getFavouritesPaginated(
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.resourcePermission IN (:permissions) AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ")" +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            ")) " +
            "ORDER BY modified DESC",
    )
    fun getWithPermissionsPaginated(
        permissions: Set<Permission>,
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.expiry IS NOT NULL AND r.expiry < :expiryTimestampMillis AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            ")) " +
            "ORDER BY expiry ASC",
    )
    fun getExpiredResourcesPaginated(
        slugs: Set<String>,
        expiryTimestampMillis: Long = ZonedDateTime.now().toInstant().toEpochMilli(),
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "INNER JOIN ResourceAndTagsCrossRef cr " +
            "ON r.resourceId=cr.resourceId " +
            "WHERE cr.tagId=:tagId AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   )" +
            ")) ",
    )
    fun getResourcesWithTag(
        tagId: String,
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "INNER JOIN ResourceAndGroupsCrossRef cr " +
            "ON r.resourceId=cr.resourceId " +
            "WHERE cr.groupId=:groupId AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   )" +
            ")) ",
    )
    fun getResourcesWithGroup(
        groupId: String,
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.folderId IS :folderId AND r.resourceTypeId IN(" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND ( " +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            "))",
    )
    fun getResourcesForFolderWithId(
        folderId: String?,
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>

    @Transaction
    @Query(
        "SELECT r.resourceId, r.folderId, r.expiry, r.favouriteId, r.modified, " +
            "r.resourcePermission, r.resourceTypeId, r.metadataKeyId, r.metadataKeyType, rm.metadataJson " +
            "FROM Resource r " +
            "INNER JOIN ResourceMetadata rm " +
            "ON r.resourceId = rm.resourceId " +
            "WHERE r.folderId IN (:inOneOfFolders) " +
            "AND r.resourceTypeId IN (" +
            "   SELECT resourceTypeId FROM ResourceType WHERE slug IN (:slugs)" +
            ") " +
            "AND (" +
            "   :ftsQuery IS NULL OR (" +
            "   EXISTS (SELECT 1 FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH :ftsQuery AND docid = rm.rowid) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM ResourceUriFts, ResourceUri " +
            "       WHERE ResourceUriFts.docid = ResourceUri.rowid AND ResourceUriFts MATCH :ftsQuery " +
            "       AND ResourceUri.resourceId = r.resourceId" +
            "   ) OR " +
            "   EXISTS (" +
            "       SELECT 1 FROM TagFts, Tag, ResourceAndTagsCrossRef rTCR " +
            "       WHERE TagFts.docid = Tag.rowid AND TagFts MATCH :ftsQuery " +
            "       AND Tag.id = rTCR.tagId AND rTCR.resourceId = r.resourceId" +
            "   )" +
            ")) " +
            "ORDER BY modified DESC",
    )
    fun getFilteredForChildFolders(
        inOneOfFolders: List<String>,
        slugs: Set<String>,
        ftsQuery: String?,
    ): PagingSource<Int, ResourceWithMetadata>
}
