package com.passbolt.mobile.android.core.resources.usecase.db

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.mappers.ResourceModelMapper
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
class GetLocalResourcesWithTagPaginatedUseCase(
    private val databaseProvider: DatabaseProvider,
    private val resourceModelMapper: ResourceModelMapper,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<GetLocalResourcesWithTagPaginatedUseCase.Input, GetLocalResourcesWithTagPaginatedUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        Output(
            Pager(
                config = PagingConfig(pageSize = input.pageSize, enablePlaceholders = false),
                pagingSourceFactory = {
                    databaseProvider
                        .get(requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount))
                        .paginatedResourcesDao()
                        .getResourcesWithTag(requireNotNull(input.tag.activeTagId), input.slugs, input.searchQuery)
                },
            ).flow.map { pagingData ->
                pagingData.map {
                    resourceModelMapper.map(it)
                }
            },
        )

    data class Input(
        val tag: HomeDisplayViewModel.Tags,
        val slugs: Set<String>,
        val searchQuery: String? = null,
        val pageSize: Int = DEFAULT_PAGE_SIZE,
    )

    data class Output(
        val resources: Flow<PagingData<ResourceModel>>,
    )

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}
