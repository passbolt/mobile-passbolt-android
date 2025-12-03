package com.passbolt.mobile.android.core.resources.usecase.db

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.ByModifiedDateDescending
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.ByNameAscending
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.HasExpiry
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.HasPermissions
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView.IsFavourite
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.homeSlugs
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
class GetLocalResourcesPaginatedUseCase(
    private val databaseProvider: DatabaseProvider,
    private val resourceModelMapper: ResourceModelMapper,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val homeDisplayViewMapper: HomeDisplayViewMapper,
) : AsyncUseCase<GetLocalResourcesPaginatedUseCase.Input, GetLocalResourcesPaginatedUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        Output(
            Pager(
                config = PagingConfig(pageSize = input.pageSize, enablePlaceholders = false),
                pagingSourceFactory = {
                    val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
                    val resourceDao = databaseProvider.get(selectedAccount).paginatedResourcesDao()

                    when (val viewType = homeDisplayViewMapper.map(input.homeDisplayView)) {
                        is ByModifiedDateDescending -> resourceDao.getAllOrderedByModifiedDatePaginated(input.slugs, input.searchQuery)
                        is ByNameAscending -> resourceDao.getAllOrderedByNamePaginated(homeSlugs, input.searchQuery)
                        is IsFavourite -> resourceDao.getFavouritesPaginated(input.slugs, input.searchQuery)
                        is HasPermissions ->
                            resourceDao.getWithPermissionsPaginated(
                                viewType.permissions,
                                input.slugs,
                                input.searchQuery,
                            )
                        is HasExpiry -> resourceDao.getExpiredResourcesPaginated(input.slugs, input.searchQuery)
                    }
                },
            ).flow.map { pagingData ->
                pagingData.map {
                    resourceModelMapper.map(it)
                }
            },
        )

    data class Input(
        val slugs: Set<String>,
        val homeDisplayView: HomeDisplayViewModel = HomeDisplayViewModel.AllItems,
        val searchQuery: String? = null,
        val pageSize: Int = DEFAULT_PAGE_SIZE,
    )

    data class Output(
        val pagedResourcesFlow: Flow<PagingData<ResourceModel>>,
    )

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}
