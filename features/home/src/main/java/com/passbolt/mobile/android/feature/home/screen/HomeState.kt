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

import com.passbolt.mobile.android.common.extension.areListsEmpty
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataWithHeader
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.ResourceModel

data class HomeState(
    val homeData: HomeDataWithHeader = HomeDataWithHeader(),
    val homeView: HomeDisplayViewModel = HomeDisplayViewModel.AllItems,
    val showSuggestedModel: ShowSuggestedModel = ShowSuggestedModel.DoNotShow,
    val canCreateResource: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val showProgress: Boolean = false,
    val searchInputEndIconMode: SearchInputEndIconMode = AVATAR,
    val userAvatar: String? = null,
    val showCreateResourceBottomSheet: Boolean = false,
    val showResourceMoreBottomSheet: Boolean = false,
    val moreMenuResource: ResourceModel? = null,
    val showAccountSwitchBottomSheet: Boolean = false,
    val showDeleteResourceConfirmationDialog: Boolean = false,
    val showFiltersBottomSheet: Boolean = false,
) {
    val shouldShowEmptyState: Boolean
        get() =
            areListsEmpty(
                homeData.data.resourceList,
                homeData.data.foldersList,
                homeData.data.tagsList,
                homeData.data.groupsList,
                homeData.data.suggestedResourceList,
            )

    val showBackIcon: Boolean
        get() =
            when (homeView) {
                is HomeDisplayViewModel.Folders -> {
                    homeView.activeFolder is Folder.Child
                }
                is HomeDisplayViewModel.Tags -> homeView.activeTagId != null
                is HomeDisplayViewModel.Groups -> homeView.activeGroupId != null
                else -> false
            }

    val showMoreMenu: Boolean
        get() = homeView is HomeDisplayViewModel.Folders && homeView.activeFolder is Folder.Child

    val requireMoreMenuResource: ResourceModel
        get() = requireNotNull(moreMenuResource)

    val currentFolderId: String?
        get() = (homeView as? Folders)?.activeFolder?.folderId
}
