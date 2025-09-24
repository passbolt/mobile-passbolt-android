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

import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel

data class OtpState(
    val otps: List<OtpItemWrapper> = emptyList(),
    val filteredOtps: List<OtpItemWrapper> = emptyList(),
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val showProgress: Boolean = false,
    val searchInputEndIconMode: SearchInputEndIconMode = SearchInputEndIconMode.AVATAR,
    val userAvatar: String? = null,
    val moreMenuResource: OtpItemWrapper? = null,
    val metadataDeletedKeyModel: TrustedKeyDeletedModel? = null,
    val newMetadataKeyTrustModel: NewMetadataKeyToTrustModel? = null,
    val showCreateResourceBottomSheet: Boolean = false,
    val showOtpMoreBottomSheet: Boolean = false,
    val showAccountSwitchBottomSheet: Boolean = false,
    val showDeleteTotpConfirmationDialog: Boolean = false,
    val showMetadataTrustedKeyDeletedDialog: Boolean = false,
    val showNewMetadataTrustDialog: Boolean = false,
) {
    val isInFilteringMode: Boolean
        get() = searchQuery.isNotEmpty()

    val shouldShowEmptyState: Boolean
        get() = uiOtps.isEmpty()

    val uiOtps: List<OtpItemWrapper>
        get() = if (isInFilteringMode) filteredOtps else otps
}
