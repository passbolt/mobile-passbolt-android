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

import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption

sealed interface HomeIntent {
    // screen
    data class Initialize(
        val showSuggestedModel: ShowSuggestedModel,
        val homeView: HomeDisplayViewModel?,
    ) : HomeIntent

    data class Search(
        val searchQuery: String,
    ) : HomeIntent

    data class ShowHomeView(
        val homeView: HomeDisplayViewModel,
    ) : HomeIntent

    data object SearchEndIconAction : HomeIntent

    // switch account
    object CloseSwitchAccount : HomeIntent

    // create resource menu
    data object OpenCreateResourceMenu : HomeIntent

    data object CreatePassword : HomeIntent

    data object CreateTotp : HomeIntent

    data object CreateNote : HomeIntent

    data object CreateFolder : HomeIntent

    data object CloseCreateResourceMenu : HomeIntent

    // return from other screen feedback
    data class OtpQRScanReturned(
        val otpCreated: Boolean,
        val otpManualCreationChosen: Boolean,
    ) : HomeIntent

    data class ResourceDetailsReturned(
        val resourceEdited: Boolean,
        val resourceDeleted: Boolean,
        val resourceName: String?,
    ) : HomeIntent

    data class ResourceShareReturned(
        val resourceShared: Boolean,
    ) : HomeIntent

    data class FolderCreateReturned(
        val folderName: String,
    ) : HomeIntent

    data class ResourceFormReturned(
        val resourceCreated: Boolean,
        val resourceEdited: Boolean,
        val resourceName: String?,
    ) : HomeIntent

    // resource more menu
    data class OpenResourceMenu(
        val resourceModel: ResourceModel,
    ) : HomeIntent

    data object CloseResourceMoreMenu : HomeIntent

    data object ConfirmDeleteResource : HomeIntent

    data object CloseDeleteConfirmationDialog : HomeIntent

    data object DeleteResource : HomeIntent

    data object LaunchResourceWebsite : HomeIntent

    data object ShareResource : HomeIntent

    data object EditResource : HomeIntent

    data object CopyResourceUri : HomeIntent

    data object CopyNote : HomeIntent

    data object CopyResourceMetadataDescription : HomeIntent

    data object CopyPassword : HomeIntent

    data object CopyResourceUsername : HomeIntent

    data class ToggleResourceFavourite(
        val option: FavouriteOption,
    ) : HomeIntent
}
