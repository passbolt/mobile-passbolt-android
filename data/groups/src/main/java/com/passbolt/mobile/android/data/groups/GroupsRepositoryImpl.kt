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

package com.passbolt.mobile.android.data.groups

import androidx.paging.PagingData
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.groups.GroupsRepository
import com.passbolt.mobile.android.domain.groups.datasource.GroupsLocalDataSource
import com.passbolt.mobile.android.domain.groups.datasource.GroupsRemoteDataSource
import com.passbolt.mobile.android.domain.groups.model.Group
import com.passbolt.mobile.android.domain.groups.model.GroupWithItemsCount
import com.passbolt.mobile.android.domain.groups.model.GroupWithMembers
import com.passbolt.mobile.android.domain.groups.model.GroupWithUsers
import kotlinx.coroutines.flow.Flow

internal class GroupsRepositoryImpl(
    private val localDataSource: GroupsLocalDataSource,
    private val remoteDataSource: GroupsRemoteDataSource,
) : GroupsRepository {
    override suspend fun getGroups(
        searchQuery: String?,
        userId: String,
    ): List<Group> = localDataSource.getGroups(searchQuery, userId)

    override fun getGroupsWithItemsCountPaged(
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<GroupWithItemsCount>> = localDataSource.getGroupsWithItemsCountPaged(searchQuery, pageSize, userId)

    override suspend fun getGroupWithUsers(
        groupId: String,
        userId: String,
    ): GroupWithUsers = localDataSource.getGroupWithUsers(groupId, userId)

    override suspend fun refreshGroups(userId: String): DomainResult<List<GroupWithMembers>> =
        remoteDataSource.getGroups().also {
            if (it is DomainResult.Finished) {
                localDataSource.upsertGroups(it.value, userId)
            }
        }

    override suspend fun fetchGroupsByIds(groupIds: List<String>): DomainResult<List<GroupWithMembers>> =
        remoteDataSource.getGroupsByIds(groupIds)
}
