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
package com.passbolt.mobile.android.feature.home.screen

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel.Show
import com.passbolt.mobile.android.feature.home.screen.data.HeaderSectionConfiguration
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TagWithCount

@Suppress("LongParameterList")
internal fun getHeaderConfig(
    resources: LazyPagingItems<ResourceModel>,
    folders: LazyPagingItems<FolderWithCountAndPath>,
    tags: LazyPagingItems<TagWithCount>,
    groups: LazyPagingItems<GroupWithCount>,
    filteredSubfolders: LazyPagingItems<FolderWithCountAndPath>,
    filteredSubfoldersResources: LazyPagingItems<ResourceModel>,
    suggestedResources: LazyPagingItems<ResourceModel>,
    searchQuery: String?,
    homeView: HomeDisplayViewModel,
    showSuggestedModel: ShowSuggestedModel,
): HeaderSectionConfiguration {
    val loadState =
        getSectionState(
            resources,
            folders,
            tags,
            groups,
            filteredSubfolders,
            filteredSubfoldersResources,
            suggestedResources,
        )
    val currentFolderName = (homeView as? Folders)?.activeFolderName

    return if (searchQuery.isNullOrBlank()) {
        HeaderSectionConfiguration(
            isInCurrentFolderSectionVisible = false,
            isInSubFoldersSectionVisible = false,
            isOtherItemsSectionVisible =
                !loadState.areAllEmpty && showSuggestedModel is Show && !suggestedResources.itemSnapshotList.isEmpty(),
            isSuggestedSectionVisible = !suggestedResources.itemSnapshotList.isEmpty(),
            currentFolderName = currentFolderName,
            areAllSectionsEmpty = loadState.areAllEmpty && !loadState.isAnyLoading,
        )
    } else {
        HeaderSectionConfiguration(
            isInCurrentFolderSectionVisible =
                homeView is Folders &&
                    !(resources.itemSnapshotList.isEmpty() && folders.itemSnapshotList.isEmpty()),
            isInSubFoldersSectionVisible =
                homeView is Folders &&
                    !(
                        filteredSubfolders.itemSnapshotList.isEmpty() &&
                            filteredSubfoldersResources.itemSnapshotList.isEmpty()
                    ),
            isSuggestedSectionVisible = false,
            isOtherItemsSectionVisible = false,
            currentFolderName = currentFolderName,
            areAllSectionsEmpty = loadState.areAllEmpty && !loadState.isAnyLoading,
        )
    }
}

private data class SectionLoadState(
    val isAnyLoading: Boolean,
    val areAllEmpty: Boolean,
)

private fun getSectionState(vararg lists: LazyPagingItems<*>): SectionLoadState {
    val isAnyLoading = lists.any { it.loadState.refresh is LoadState.Loading }
    val areAllEmpty = lists.all { it.itemSnapshotList.isEmpty() }
    return SectionLoadState(isAnyLoading, areAllEmpty)
}
