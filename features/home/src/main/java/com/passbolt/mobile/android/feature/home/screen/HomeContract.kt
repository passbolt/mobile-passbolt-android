package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel

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
interface HomeContract {

    interface View : BaseAuthenticatedContract.View {
        fun showPasswords(list: List<ResourceModel>)
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
        fun hideUpdateButton()
        fun showAddButton()
        fun showDeleteConfirmationDialog()
        fun navigateToManageAccounts()
    }

    interface Presenter : BaseAuthenticatedContract.Presenter<View> {
        fun moreClick(resourceModel: ResourceModel)
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
        fun newResourceCreated()
        fun menuDeleteClick()
        fun resourceDeleted(resourceName: String)
        fun menuEditClick()
        fun resourceEdited(resourceName: String)
        fun deleteResourceConfirmed()
        fun switchAccountManageAccountClick()
    }
}
