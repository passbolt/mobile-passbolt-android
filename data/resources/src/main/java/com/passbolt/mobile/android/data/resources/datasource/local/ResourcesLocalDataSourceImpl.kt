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
package com.passbolt.mobile.android.data.resources.datasource.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.passbolt.mobile.android.data.resources.mapper.toResourceDatabaseView
import com.passbolt.mobile.android.data.resources.mapper.toUiModel
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.database.QuerySanitizer
import com.passbolt.mobile.android.domain.resources.ResourcesLocalDataSource
import com.passbolt.mobile.android.domain.resources.mapper.toDomain
import com.passbolt.mobile.android.domain.resources.mapper.toUiModel
import com.passbolt.mobile.android.domain.resources.model.Resource
import com.passbolt.mobile.android.domain.resources.model.ResourceWithAttributes
import com.passbolt.mobile.android.entity.group.ResourceAndGroupsCrossRef
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.ByModifiedDateDescending
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.ByNameAscending
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.HasExpiry
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.HasPermissions
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.IsFavourite
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState.UPDATED
import com.passbolt.mobile.android.entity.user.ResourceAndUsersCrossRef
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.TagModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("TooManyFunctions")
internal class ResourcesLocalDataSourceImpl(
    private val databaseProvider: DatabaseProvider,
    private val resourceModelMapper: ResourceModelMapper,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val querySanitizer: QuerySanitizer,
) : ResourcesLocalDataSource {
    override suspend fun getLocalResource(
        resourceId: String,
        userId: String,
    ): Resource =
        databaseProvider
            .get(userId)
            .resourcesDao()
            .get(resourceId)
            .let { resourceModelMapper.map(it).toDomain() }

    override suspend fun getLocalResourcePermissions(
        resourceId: String,
        userId: String,
    ): List<PermissionModelUi> {
        val resourcesDao =
            databaseProvider
                .get(userId)
                .resourcesDao()

        val groupsPermissions = resourcesDao.getResourceGroupsPermissions(resourceId)
        val usersPermissions = resourcesDao.getResourceUsersPermissions(resourceId)

        return permissionsModelMapper.map(groupsPermissions, usersPermissions)
    }

    override suspend fun getLocalResourceTags(
        resourceId: String,
        userId: String,
    ): List<TagModel> =
        databaseProvider
            .get(userId)
            .tagsDao()
            .getResourceTags(resourceId)
            .map { it.toUiModel() }

    override suspend fun getLocalResourcesFilteredByTag(
        tagSearchQuery: String,
        slugs: Set<String>,
        userId: String,
    ): List<Resource> {
        val resources =
            databaseProvider
                .get(userId)
                .resourcesDao()
                .getAllThatHaveTagContaining(slugs, querySanitizer.sanitize(tagSearchQuery))

        return resources.map { resourceModelMapper.map(it).toDomain() }
    }

    override suspend fun getLocalResources(
        slugs: Set<String>,
        homeDisplayView: HomeDisplayViewModel,
        searchQuery: String?,
        userId: String,
    ): List<Resource> {
        val ftsQuery = querySanitizer.sanitize(searchQuery)
        val resources =
            databaseProvider
                .get(userId)
                .resourcesDao()
                .let {
                    when (val viewType = homeDisplayView.toResourceDatabaseView()) {
                        is ResourceDatabaseView.ByModifiedDateDescending -> it.getAllOrderedByModifiedDate(slugs, ftsQuery)
                        is ResourceDatabaseView.ByNameAscending -> it.getAllOrderedByName(slugs, ftsQuery)
                        is ResourceDatabaseView.IsFavourite -> it.getFavourites(slugs, ftsQuery)
                        is ResourceDatabaseView.HasPermissions ->
                            it.getWithPermissions(
                                viewType.permissions,
                                slugs,
                                ftsQuery,
                            )
                        is ResourceDatabaseView.HasExpiry -> it.getExpiredResources(slugs, ftsQuery = ftsQuery)
                    }
                }

        return resources.map { resourceModelMapper.map(it).toDomain() }
    }

    override suspend fun getLocalResourcesWithGroup(
        group: HomeDisplayViewModel.Groups,
        slugs: Set<String>,
        searchQuery: String?,
        userId: String,
    ): List<Resource> {
        val resources =
            databaseProvider
                .get(userId)
                .resourcesDao()
                .getResourcesWithGroup(
                    requireNotNull(group.activeGroupId),
                    slugs,
                    querySanitizer.sanitize(searchQuery),
                )

        return resources.map { resourceModelMapper.map(it).toDomain() }
    }

    override suspend fun getLocalResourcesWithTag(
        tag: HomeDisplayViewModel.Tags,
        slugs: Set<String>,
        searchQuery: String?,
        userId: String,
    ): List<Resource> {
        val resources =
            databaseProvider
                .get(userId)
                .resourcesDao()
                .getResourcesWithTag(
                    requireNotNull(tag.activeTagId),
                    slugs,
                    querySanitizer.sanitize(searchQuery),
                )

        return resources.map { resourceModelMapper.map(it).toDomain() }
    }

    override fun getLocalResourcesPaginated(
        slugs: Set<String>,
        homeDisplayView: HomeDisplayViewModel,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<Resource>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = enablePlaceholders),
            pagingSourceFactory = {
                val resourceDao = databaseProvider.get(userId).paginatedResourcesDao()
                val ftsQuery = querySanitizer.sanitize(searchQuery)

                when (val viewType = homeDisplayView.toResourceDatabaseView()) {
                    is ByModifiedDateDescending ->
                        resourceDao.getAllOrderedByModifiedDatePaginated(slugs, ftsQuery)
                    is ByNameAscending -> resourceDao.getAllOrderedByNamePaginated(slugs, ftsQuery)
                    is IsFavourite -> resourceDao.getFavouritesPaginated(slugs, ftsQuery)
                    is HasPermissions ->
                        resourceDao.getWithPermissionsPaginated(
                            viewType.permissions,
                            slugs,
                            ftsQuery,
                        )
                    is HasExpiry -> resourceDao.getExpiredResourcesPaginated(slugs, ftsQuery = ftsQuery)
                }
            },
        ).flow.map { pagingData ->
            pagingData.map {
                resourceModelMapper.map(it).toDomain()
            }
        }

    override fun getLocalResourcesWithGroupPaginated(
        group: HomeDisplayViewModel.Groups,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<Resource>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                databaseProvider
                    .get(userId)
                    .paginatedResourcesDao()
                    .getResourcesWithGroup(
                        requireNotNull(group.activeGroupId),
                        slugs,
                        querySanitizer.sanitize(searchQuery),
                    )
            },
        ).flow.map { pagingData ->
            pagingData.map {
                resourceModelMapper.map(it).toDomain()
            }
        }

    override fun getLocalResourcesWithTagPaginated(
        tag: HomeDisplayViewModel.Tags,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<Resource>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                databaseProvider
                    .get(userId)
                    .paginatedResourcesDao()
                    .getResourcesWithTag(
                        requireNotNull(tag.activeTagId),
                        slugs,
                        querySanitizer.sanitize(searchQuery),
                    )
            },
        ).flow.map { pagingData ->
            pagingData.map {
                resourceModelMapper.map(it).toDomain()
            }
        }

    override fun getResourcesInFolderPaged(
        folderId: String?,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<Resource>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = enablePlaceholders),
            pagingSourceFactory = {
                databaseProvider
                    .get(userId)
                    .paginatedResourcesDao()
                    .getResourcesForFolderWithId(folderId, slugs, querySanitizer.sanitize(searchQuery))
            },
        ).flow.map { pagingData ->
            pagingData.map {
                resourceModelMapper.map(it).toDomain()
            }
        }

    override fun getSubFolderResourcesFilteredPaged(
        containingFolders: List<String>,
        containingQuery: String,
        slugs: Set<String>,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<Resource>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                databaseProvider
                    .get(userId)
                    .paginatedResourcesDao()
                    .getFilteredForChildFolders(containingFolders, slugs, querySanitizer.sanitize(containingQuery))
            },
        ).flow.map { pagingData ->
            pagingData.map {
                resourceModelMapper.map(it).toDomain()
            }
        }

    override suspend fun addLocalResource(
        resource: Resource,
        userId: String,
    ) {
        val uiResource = resource.toUiModel()
        val db = databaseProvider.get(userId)
        val resourcesDao = db.resourcesDao()
        val resourceMetadataDao = db.resourceMetadataDao()
        val resourceUriDao = db.resourceUriDao()

        resourcesDao.insert(resourceModelMapper.map(uiResource, resourceUpdateState = UPDATED))
        resourceMetadataDao.insert(resourceModelMapper.mapResourceMetadata(uiResource))
        resourceUriDao.insertAll(resourceModelMapper.mapResourceUris(uiResource))
    }

    override suspend fun addLocalResourcePermissions(
        resources: List<ResourceWithAttributes>,
        userId: String,
    ) {
        val db = databaseProvider.get(userId)
        val resourcesAndGroupsCrossRefDao = db.resourcesAndGroupsCrossRefDao()
        val resourcesAndUsersCrossRefDao = db.resourcesAndUsersCrossRefDao()

        resources.apply {
            val resourceGroupPermissions =
                map {
                    it.resource.resourceId to
                        it.resourcePermissions
                            .filterIsInstance<PermissionModel.GroupPermissionModel>()
                }
            val resourceUserPermissions =
                map {
                    it.resource.resourceId to
                        it.resourcePermissions
                            .filterIsInstance<PermissionModel.UserPermissionModel>()
                }

            val resourceAndGroupCrossRefs =
                resourceGroupPermissions
                    .flatMap { (resourceId, groupPermissions) ->
                        groupPermissions.map { groupPermission ->
                            ResourceAndGroupsCrossRef(
                                resourceId,
                                groupPermission.group.groupId,
                                permissionsModelMapper.map(groupPermission.permission),
                                groupPermission.permissionId,
                            )
                        }
                    }

            val resourceAndUsersCrossRefs =
                resourceUserPermissions
                    .flatMap { (resourceId, userPermissions) ->
                        userPermissions.map { userPermission ->
                            ResourceAndUsersCrossRef(
                                resourceId,
                                userPermission.userId,
                                permissionsModelMapper.map(userPermission.permission),
                                userPermission.permissionId,
                            )
                        }
                    }

            resourcesAndGroupsCrossRefDao.insertAll(resourceAndGroupCrossRefs)
            resourcesAndUsersCrossRefDao.insertAll(resourceAndUsersCrossRefs)
        }
    }

    override suspend fun updateLocalResource(
        resource: Resource,
        userId: String,
    ) {
        val uiResource = resource.toUiModel()
        val db = databaseProvider.get(userId)
        val resourcesDao = db.resourcesDao()
        val resourceMetadataDao = db.resourceMetadataDao()
        val resourceUriDao = db.resourceUriDao()

        resourcesDao.update(resourceModelMapper.map(uiResource, resourceUpdateState = UPDATED))
        resourceMetadataDao.updateMetadataForResource(
            resourceId = uiResource.resourceId,
            metadataJson = requireNotNull(uiResource.metadataJsonModel.json),
            name = uiResource.metadataJsonModel.name,
            username = uiResource.metadataJsonModel.username,
            description = uiResource.metadataJsonModel.description,
            customFieldsKeys =
                uiResource.metadataJsonModel.customFields
                    ?.joinToString(),
        )

        resourceUriDao.apply {
            deleteForResource(uiResource.resourceId)
            insertAll(resourceModelMapper.mapResourceUris(uiResource))
        }
    }

    override suspend fun upsertLocalResources(
        resources: List<Resource>,
        userId: String,
    ) {
        val uiResources = resources.map { it.toUiModel() }
        val db = databaseProvider.get(userId)
        val resourcesDao = db.resourcesDao()
        val resourceMetadataDao = db.resourceMetadataDao()
        val resourceUriDao = db.resourceUriDao()

        val resourceEntities = uiResources.map { resourceModelMapper.map(it, resourceUpdateState = UPDATED) }
        val resourceMetadata = uiResources.map { resourceModelMapper.mapResourceMetadata(it) }
        val resourceUris = uiResources.map { resourceModelMapper.mapResourceUris(it) }

        resourcesDao.upsertAll(resourceEntities)
        resourceMetadataDao.upsertAll(resourceMetadata)
        // URIs are still deleted and re-inserted
        resourceUriDao.insertAll(resourceUris.flatten())
    }

    override suspend fun removeLocalResourcePermissions(userId: String) {
        databaseProvider
            .get(userId)
            .apply {
                resourcesAndGroupsCrossRefDao().deleteAll()
                resourcesAndUsersCrossRefDao().deleteAll()
            }
    }

    override suspend fun removeLocalUris(userId: String) {
        databaseProvider
            .get(userId)
            .resourceUriDao()
            .deleteAll()
    }

    override suspend fun removeLocalResourcesWithUpdateState(
        updateState: ResourceUpdateState,
        userId: String,
    ) {
        databaseProvider
            .get(userId)
            .resourcesDao()
            .removeWithUpdateState(updateState)
    }

    override suspend fun setLocalResourcesUpdateState(
        updateState: ResourceUpdateState,
        userId: String,
    ) {
        databaseProvider
            .get(userId)
            .resourcesDao()
            .setAllUpdateState(updateState)
    }
}
