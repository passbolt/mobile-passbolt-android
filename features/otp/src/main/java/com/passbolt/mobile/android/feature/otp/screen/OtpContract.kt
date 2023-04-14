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
import com.passbolt.mobile.android.ui.OtpListItemWrapper
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel

interface OtpContract {

    interface View : DataRefreshViewReactiveContract.View {
        fun showOtpList(otpList: List<OtpListItemWrapper>)
        fun showEmptyView()
        fun hideEmptyView()
        fun performRefreshUsingRefreshExecutor()
        fun displaySearchAvatar(avatarUrl: String?)
        fun navigateToSwitchAccount(appContext: AppContext)
        fun showFullscreenError()
        fun hideFullScreenError()
        fun navigateToManageAccounts()
        fun navigateToSwitchedAccountAuth(appContext: AppContext)
        fun showPleaseWaitForDataRefresh()
        fun displaySearchClearIcon()
        fun clearSearchInput()
        fun showOtmMoreMenu(moreMenuModel: ResourceMoreMenuModel)
        fun copySecretToClipBoard(label: String, value: String)
        fun navigateToScanOtpQrCode()
        fun navigateToCreateOtpManually()
        fun showDecryptionFailure()
        fun showFetchFailure()
        fun showConfirmDeleteDialog()
        fun showFailedToDeleteResource()
        fun showResourceDeleted()
        fun initRefresh()
        fun showNewOtpCreated()
    }

    interface Presenter : DataRefreshViewReactiveContract.Presenter<View> {
        fun otpItemClick(otpListItemWrapper: OtpListItemWrapper)
        fun otpItemMoreClick(otpListWrapper: OtpListItemWrapper)
        fun refreshClick()
        fun searchAvatarClick()
        fun switchAccountManageAccountClick()
        fun switchAccountClick()
        fun searchClearClick()
        fun searchTextChanged(text: String)
        fun menuCopyOtpClick()
        fun menuShowOtpClick()
        fun menuDeleteOtpClick()
        fun menuEditOtpClick()
        fun scanOtpQrCodeClick()
        fun createOtpManuallyClick()
        fun totpDeletetionConfirmed()
        fun otpCreated()
    }
}
