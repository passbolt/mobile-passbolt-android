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

package com.passbolt.mobile.android.feature.otp.screen

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactiveContract
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel

interface OtpContract {

    @Suppress("TooManyFunctions")
    interface View : DataRefreshViewReactiveContract.View {
        fun showOtpList(otpList: List<OtpItemWrapper>)
        fun showEmptyView()
        fun hideEmptyView()
        fun displaySearchAvatar(avatarUrl: String?)
        fun navigateToSwitchAccount(appContext: AppContext)
        fun navigateToManageAccounts()
        fun navigateToSwitchedAccountAuth(appContext: AppContext)
        fun showPleaseWaitForDataRefresh()
        fun displaySearchClearIcon()
        fun clearSearchInput()
        fun showOtmMoreMenu(resourceId: String, resourceName: String)
        fun copySecretToClipBoard(label: String, value: String)
        fun showDecryptionFailure()
        fun showFetchFailure()
        fun showConfirmDeleteDialog()
        fun showFailedToDeleteResource()
        fun showResourceDeleted()
        fun showNewOtpCreated()
        fun showProgress()
        fun hideProgress()
        fun showOtpUpdate()
        fun navigateToScanOtpCodeForResult()
        fun showError(message: String)
        fun showEncryptionError(message: String)
        fun showCreateButton()
        fun hideCreateButton()
        fun showTotpDeleted()
        fun showDataRefreshError()
        fun showJsonResourceSchemaValidationError()
        fun showJsonSecretSchemaValidationError()
        fun navigateToCreateTotpManually()
        fun navigateToEditResource(resourceModel: ResourceModel)
        fun showResourceCreatedSnackbar()
        fun showResourceEditedSnackbar(resourceName: String)
        fun showCannotUpdateTotpWithCurrentConfig()
        fun showMetadataKeyModifiedDialog(model: NewMetadataKeyToTrustModel)
        fun showMetadataKeyDeletedDialog(model: TrustedKeyDeletedModel)
        fun showFailedToVerifyMetadataKey()
        fun showNewMetadataKeyIsTrusted()
        fun showFailedToTrustMetadataKey()
    }

    interface Presenter : DataRefreshViewReactiveContract.Presenter<View> {
        fun otpItemClick(otpItemWrapper: OtpItemWrapper)
        fun otpItemMoreClick(otpListWrapper: OtpItemWrapper)
        fun refreshClick()
        fun searchAvatarClick()
        fun switchAccountManageAccountClick()
        fun switchAccountClick()
        fun searchClearClick()
        fun searchTextChanged(text: String)
        fun menuCopyOtpClick()
        fun menuShowOtpClick()
        fun menuDeleteOtpClick()
        fun totpDeletionConfirmed()
        fun otpCreated()
        fun otpUpdated()
        fun otpQrScanReturned(isTotpCreated: Boolean, isManualCreationChosen: Boolean)
        fun menuEditOtpClick()
        fun resourceFormReturned(isResourceCreated: Boolean, isResourceEdited: Boolean, resourceName: String?)
        fun trustedMetadataKeyDeleted(model: TrustedKeyDeletedModel)
        fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel)
    }
}
