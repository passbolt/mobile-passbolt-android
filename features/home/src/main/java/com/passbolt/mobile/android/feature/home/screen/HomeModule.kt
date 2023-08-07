package com.passbolt.mobile.android.feature.home.screen

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.feature.home.screen.recycler.FolderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.GroupWithCountItem
import com.passbolt.mobile.android.feature.home.screen.recycler.InCurrentFoldersHeaderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.InSubFoldersHeaderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.PasswordHeaderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.PasswordItem
import com.passbolt.mobile.android.feature.home.screen.recycler.TagWithCountItem
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
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

internal const val SUGGESTED_HEADER_ITEM_ADAPTER = "SUGGESTED_HEADER_ITEM_ADAPTER"
internal const val SUGGESTED_ITEMS_ITEM_ADAPTER = "SUGGESTED_ITEMS_ITEM_ADAPTER"
internal const val OTHER_ITEMS_HEADER_ITEM_ADAPTER = "OTHER_ITEMS_HEADER_ITEM_ADAPTER"
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
                searchableMatcher = SearchableMatcher(),
                createResourceMenuModelUseCase = get(),
                getLocalResourcesUseCase = get(),
                getLocalResourcesFilteredByTag = get(),
                getLocalSubFoldersForFolderUseCase = get(),
                getLocalResourcesAndFoldersUseCase = get(),
                getLocalResourcesFiltered = get(),
                getLocalTagsUseCase = get(),
                getLocalResourcesWithTagUseCase = get(),
                getLocalGroupsWithShareItemsCountUseCase = get(),
                getLocalResourcesWithGroupsUseCase = get(),
                getHomeDisplayViewPrefsUseCase = get(),
                homeModelMapper = get(),
                domainProvider = get(),
                getLocalFolderUseCase = get(),
                deleteResourceIdlingResource = get(),
                totpParametersProvider = get(),
                resourceTypeFactory = get(),
                createOtpMoreMenuModelUseCase = get()
            )
        }
        scopedOf(::HomeSpeedDialFabFactory)
        declareHomeListAdapters()
        declareHomeListItemAdapters()
    }
}

private fun ScopeDSL.declareHomeListItemAdapters() {
    scoped {
        FastAdapter.with(
            listOf(
                get<ItemAdapter<PasswordHeaderItem>>(named(SUGGESTED_HEADER_ITEM_ADAPTER)),
                get<ItemAdapter<PasswordItem>>(named(SUGGESTED_ITEMS_ITEM_ADAPTER)),
                get<ItemAdapter<PasswordHeaderItem>>(named(OTHER_ITEMS_HEADER_ITEM_ADAPTER)),
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

fun ScopeDSL.declareHomeListAdapters() {
    scoped<ItemAdapter<PasswordHeaderItem>>(named(SUGGESTED_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<PasswordItem>>(named(SUGGESTED_ITEMS_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<PasswordHeaderItem>>(named(OTHER_ITEMS_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
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
