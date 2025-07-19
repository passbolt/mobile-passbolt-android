package com.passbolt.mobile.android.feature.resourcedetails.details

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactiveContract
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import java.time.ZonedDateTime

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
interface ResourceDetailsContract {
    @Suppress("TooManyFunctions")
    interface View : DataRefreshViewReactiveContract.View {
        fun displayTitle(title: String)

        fun displayExpiryTitle(name: String)

        fun displayUsername(username: String)

        fun addToClipboard(
            label: String,
            value: String,
            isSecret: Boolean,
        )

        fun displayUrl(url: String)

        fun displayInitialsIcon(resource: ResourceModel)

        fun navigateToMore(
            resourceId: String,
            resourceName: String,
        )

        fun navigateBack()

        fun showProgress()

        fun hideProgress()

        fun showDecryptionFailure()

        fun showFetchFailure()

        fun hidePassword()

        fun showPassword(decryptedSecret: String)

        fun clearPasswordInput()

        fun showNote(note: String)

        fun showPasswordEyeIcon()

        fun openWebsite(url: String)

        fun showGeneralError(errorMessage: String? = null)

        fun closeWithDeleteSuccessResult(name: String)

        fun navigateToEditResource(resourceModel: ResourceModel)

        fun showResourceEditedSnackbar(resourceName: String)

        fun showDeleteConfirmationDialog()

        fun showPermissions(
            groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
            userPermissions: List<PermissionModelUi.UserPermissionModel>,
            counterValue: List<String>,
            overlapOffset: Int,
        )

        fun navigateToResourcePermissions(
            resourceId: String,
            mode: PermissionsMode,
        )

        fun setResourceEditedResult(resourceName: String)

        fun showResourceSharedSnackbar()

        fun showToggleFavouriteFailure()

        fun showFavouriteStar()

        fun hideFavouriteStar()

        fun showTags(tags: List<String>)

        fun navigateToResourceTags(
            resourceId: String,
            mode: PermissionsMode,
        )

        fun showFolderLocation(locationPathSegments: List<String>)

        fun navigateToResourceLocation(resourceId: String)

        fun showDataRefreshError()

        fun showContentNotAvailable()

        fun showTotp(otpWrapper: OtpItemWrapper?)

        fun showTotpSection()

        fun showInvalidTotpScanned()

        fun showEncryptionError(message: String)

        fun hideTotpSection()

        fun showTotpDeleted()

        fun hidePasswordEyeIcon()

        fun clearNoteInput()

        fun disableNote()

        fun displayExpirySection(expiry: ZonedDateTime)

        fun showExpiryIndicator()

        fun hideExpirySection()

        fun showMetadataDescription(description: String)

        fun disableMetadataDescription()

        fun hideNote()

        fun showPasswordSection()

        fun hidePasswordSection()

        fun hideSharedWith()

        fun hideTags()

        fun hideLocation()

        fun displayAdditionalUrls(uris: List<String>)
    }

    @Suppress("TooManyFunctions")
    interface Presenter : DataRefreshViewReactiveContract.Presenter<View> {
        fun argsReceived(
            resourceModel: ResourceModel,
            permissionsListWidth: Int,
            permissionItemWidth: Float,
        )

        fun usernameCopyClick()

        fun urlCopyClick()

        fun moreClick()

        fun backArrowClick()

        fun passwordActionClick()

        fun viewStopped()

        fun metadataDescriptionActionClick()

        fun resourceEdited(resourceName: String?)

        fun deleteResourceConfirmed()

        fun sharedWithClick()

        fun resourceShared()

        fun favouriteClick(option: ResourceMoreMenuModel.FavouriteOption)

        fun copyPasswordClick()

        fun copyMetadataDescriptionClick()

        fun launchWebsiteClick()

        fun deleteClick()

        fun editClick()

        fun shareClick()

        fun tagsClick()

        fun locationClick()

        fun totpIconClick()

        fun copyTotpClick()

        fun copyNoteClick()

        fun noteActionClick()
    }
}
