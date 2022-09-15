package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import com.passbolt.mobile.android.ui.FolderWithCount
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.TagWithCount
import kotlinx.coroutines.flow.Flow

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
@Suppress("TooManyFunctions", "LongParameterList") // TODO MOB-321
interface HomeContract {

    interface View : BaseAuthenticatedContract.View, ResourceHandlingStrategy {
        fun showItems(
            suggestedResources: List<ResourceModel>,
            resourceList: List<ResourceModel>,
            foldersList: List<FolderWithCount>,
            tagsList: List<TagWithCount>,
            groupsList: List<GroupWithCount>,
            filteredSubFoldersList: List<FolderWithCount>,
            filteredSubFolderResourceList: List<ResourceModel>,
            sectionsConfiguration: HomeFragment.HeaderSectionConfiguration
        )

        fun navigateToMore(resourceMoreMenuModel: ResourceMoreMenuModel)
        fun navigateToDetails(resourceModel: ResourceModel)
        fun hideProgress()
        fun showProgress()
        fun hideRefreshProgress()
        fun showError()
        fun showEmptyList()
        fun showSearchEmptyList()
        fun displaySearchAvatar(url: String?)
        fun addToClipboard(label: String, value: String)
        fun openWebsite(url: String)
        fun showDecryptionFailure()
        fun showFetchFailure()
        fun navigateToSwitchAccount()
        fun displaySearchClearIcon()
        fun clearSearchInput()
        fun showResourceAddedSnackbar()
        fun showResourceDeletedSnackbar(name: String)
        fun showGeneralError()
        fun navigateToEdit(resourceModel: ResourceModel)
        fun showResourceEditedSnackbar(resourceName: String)
        fun hideAddButton()
        fun showAddButton()
        fun showDeleteConfirmationDialog()
        fun navigateToManageAccounts()
        fun showFiltersMenu(activeDisplayView: HomeDisplayViewModel)
        fun showHomeScreenTitle(view: HomeDisplayViewModel)
        fun navigateToChild(homeView: HomeDisplayViewModel)
        fun showBackArrow()
        fun hideBackArrow()
        fun navigateToRootHomeFromChildHome(homeView: HomeDisplayViewModel)
        fun performRefreshUsingRefreshExecutor()
        fun navigateRootHomeFromRootHome(homeView: HomeDisplayViewModel)
        fun navigateToCreateResource(parentFolderId: String?)
        fun showChildFolderTitle(activeFolderName: String, isShared: Boolean)
        fun showTagTitle(activeTagTitle: String, isShared: Boolean)
        fun showGroupTitle(groupName: String)
        fun navigateToEditResourcePermissions(resource: ResourceModel)
        fun showResourceSharedSnackbar()
        fun showAllItemsSearchHint()
        fun showDefaultSearchHint()
        fun showCloseButton()
        fun navigateToHome()
        fun showToggleFavouriteFailure()
        fun performLocalRefresh()
        fun navigateToSwitchedAccountAuth(userId: String)
        fun navigateToFolderMoreMenu(folderMoreMenuModel: FolderMoreMenuModel)
        fun showFolderMoreMenuIcon()
        fun hideFolderMoreMenuIcon()
        fun navigateToFolderDetails(childFolder: Folder.Child)
        fun initSpeedDialFab(homeView: HomeDisplayViewModel)
    }

    interface Presenter : BaseAuthenticatedContract.Presenter<View> {
        fun resourceMoreClick(resourceModel: ResourceModel)
        fun itemClick(resourceModel: ResourceModel)
        fun refreshSwipe()
        fun refreshClick()
        fun searchTextChange(text: String)
        fun menuLaunchWebsiteClick()
        fun menuCopyUsernameClick()
        fun menuCopyUrlClick()
        fun menuCopyPasswordClick()
        fun searchAvatarClick()
        fun userAuthenticated()
        fun searchClearClick()
        fun menuCopyDescriptionClick()
        fun newResourceCreated(resourceId: String?)
        fun menuDeleteClick()
        fun resourceDeleted(resourceName: String)
        fun menuEditClick()
        fun resourceEdited(resourceName: String)
        fun deleteResourceConfirmed()
        fun switchAccountManageAccountClick()
        fun filtersClick()
        fun allItemsClick()
        fun favouritesClick()
        fun recentlyModifiedClick()
        fun sharedWithMeClick()
        fun ownedByMeClick()
        fun foldersClick()
        fun argsRetrieved(
            showSuggestedModel: ShowSuggestedModel,
            homeDisplayView: HomeDisplayViewModel?,
            hasPreviousEntry: Boolean,
            shouldShowCloseButton: Boolean,
            shouldShowResourceMoreMenu: Boolean
        )

        fun folderItemClick(folderModel: FolderWithCount)
        fun viewCreate(fullDataRefreshStatusFlow: Flow<DataRefreshStatus.Finished>)
        fun createResourceClick()
        fun tagsClick()
        fun tagItemClick(tag: TagWithCount)
        fun groupsClick()
        fun groupItemClick(group: GroupWithCount)
        fun menuShareClick()
        fun resourceShared()
        fun closeClick()
        fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption)
        fun switchAccountClick(userId: String)
        fun moreClick()
        fun seeFolderDetailsClick()
    }
}
