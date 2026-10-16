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

package com.passbolt.mobile.android.data.users

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.users.UsersDataSource
import com.passbolt.mobile.android.domain.users.UsersLocalDataSource
import com.passbolt.mobile.android.domain.users.UsersRepository
import com.passbolt.mobile.android.domain.users.model.UserProfile

internal class UsersRepositoryImpl(
    private val localDataSource: UsersLocalDataSource,
    private val remoteDataSource: UsersDataSource,
) : UsersRepository {
    override suspend fun getMyProfile(): DomainResult<UserProfile> = remoteDataSource.getMyProfile()

    override suspend fun getUsers(hasAccessTo: List<String>?): DomainResult<List<UserProfile>> = remoteDataSource.getUsers(hasAccessTo)

    override suspend fun getUsersByIds(userIds: List<String>): DomainResult<List<UserProfile>> = remoteDataSource.getUsersByIds(userIds)

    override suspend fun getLocalUser(
        selectedAccountId: String,
        userId: String,
    ): UserProfile = localDataSource.getUser(selectedAccountId, userId)

    override suspend fun getLocalCurrentUser(
        selectedAccountId: String,
        serverId: String,
    ): UserProfile = localDataSource.getCurrentUser(selectedAccountId, serverId)

    override suspend fun getLocalUsers(
        selectedAccountId: String,
        searchQuery: String?,
    ): List<UserProfile> = localDataSource.getUsers(selectedAccountId, searchQuery)

    override suspend fun refreshUsers(selectedAccountId: String): DomainResult<List<UserProfile>> =
        remoteDataSource.getUsers().also {
            if (it is DomainResult.Finished) {
                localDataSource.upsertUsers(selectedAccountId, it.value)
            }
        }
}
