package com.passbolt.mobile.android.feature.home.screen

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.FolderItem
import com.passbolt.mobile.android.core.commonresource.GroupWithCountItem
import com.passbolt.mobile.android.core.commonresource.InCurrentFoldersHeaderItem
import com.passbolt.mobile.android.core.commonresource.InSubFoldersHeaderItem
import com.passbolt.mobile.android.core.commonresource.PasswordItem
import com.passbolt.mobile.android.core.commonresource.TagWithCountItem
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.ScopeDSL

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

internal const val RESOURCE_ITEM_ADAPTER = "RESOURCE_ITEM_ADAPTER"
internal const val SUB_RESOURCE_ITEM_ADAPTER = "SUB_RESOURCE_ITEM_ADAPTER"
internal const val FOLDER_ITEM_ADAPTER = "FOLDER_ITEM_ADAPTER"
internal const val SUB_FOLDER_ITEM_ADAPTER = "SUB_FOLDER_ITEM_ADAPTER"
internal const val IN_SUB_FOLDERS_HEADER_ITEM_ADAPTER = "IN_SUB_FOLDERS_HEADER_ITEM_ADAPTER"
internal const val IN_CURRENT_FOLDER_HEADER_ITEM_ADAPTER = "IN_CURRENT_FOLDER_HEADER_ITEM_ADAPTER"
internal const val TAGS_ITEM_ADAPTER = "TAGS_ITEM_ADAPTER"
internal const val GROUPS_ITEM_ADAPTER = "GROUPS_ITEM_ADAPTER"

fun Module.homeModule() {
    scope<HomeFragment> {
        scoped<HomeContract.Presenter> {
            HomePresenter(
                coroutineLaunchContext = get(),
                getSelectedAccountDataUseCase = get(),
                secretInteractor = get(),
                searchableMatcher = SearchableMatcher(),
                resourceTypeFactory = get(),
                secretParser = get(),
                resourceMenuModelMapper = get(),
                deleteResourceUseCase = get(),
                getLocalResourcesUseCase = get(),
                getLocalSubFoldersForFolderUseCase = get(),
                getLocalResourcesAndFoldersUseCase = get(),
                getLocalResourcesFiltered = get(),
                getLocalTagsUseCase = get(),
                getLocalResourcesWithTagUseCase = get(),
                getLocalGroupsWithShareItemsCountUseCase = get(),
                getLocalResourcesWithGroupsUseCase = get(),
                getLocalResourcesFilteredByTag = get(),
                homeModelMapper = get(),
                getHomeDisaplyViewPrefsUseCase = get()
            )
        }
        declareHomeListAdapters()
        scoped {
            FastAdapter.with(
                listOf(
                    get<ItemAdapter<InCurrentFoldersHeaderItem>>(named(IN_CURRENT_FOLDER_HEADER_ITEM_ADAPTER)),
                    get<ItemAdapter<FolderItem>>(named(FOLDER_ITEM_ADAPTER)),
                    get<ItemAdapter<TagWithCountItem>>(named(TAGS_ITEM_ADAPTER)),
                    get<ItemAdapter<GroupWithCountItem>>(named(GROUPS_ITEM_ADAPTER)),
                    get<ItemAdapter<PasswordItem>>(named(RESOURCE_ITEM_ADAPTER)),
                    get<ItemAdapter<InSubFoldersHeaderItem>>(named(IN_SUB_FOLDERS_HEADER_ITEM_ADAPTER)),
                    get<ItemAdapter<FolderItem>>(named(SUB_FOLDER_ITEM_ADAPTER)),
                    get<ItemAdapter<PasswordItem>>(named(SUB_RESOURCE_ITEM_ADAPTER))
                )
            )
        }
    }
}

fun ScopeDSL.declareHomeListAdapters() {
    scoped<ItemAdapter<PasswordItem>>(named(RESOURCE_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<FolderItem>>(named(FOLDER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<InSubFoldersHeaderItem>>(named(IN_SUB_FOLDERS_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<PasswordItem>>(named(SUB_RESOURCE_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<FolderItem>>(named(SUB_FOLDER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<InCurrentFoldersHeaderItem>>(named(IN_CURRENT_FOLDER_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<TagWithCountItem>>(named(TAGS_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<GroupWithCountItem>>(named(GROUPS_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
}
