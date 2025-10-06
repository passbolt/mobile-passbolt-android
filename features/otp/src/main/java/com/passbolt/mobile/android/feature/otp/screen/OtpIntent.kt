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

import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper

sealed interface OtpIntent {
    // screen
    data class Search(
        val searchQuery: String,
    ) : OtpIntent

    data class RevealOtp(
        val otpItemWrapper: OtpItemWrapper,
    ) : OtpIntent

    data class OtpQRScanReturned(
        val otpCreated: Boolean,
        val otpManualCreationChosen: Boolean,
    ) : OtpIntent

    data class ResourceFormReturned(
        val resourceCreated: Boolean,
        val resourceEdited: Boolean,
        val resourceName: String?,
    ) : OtpIntent

    data object SearchEndIconAction : OtpIntent

    // switch account
    object CloseSwitchAccount : OtpIntent

    // create resource menu
    data object OpenCreateResourceMenu : OtpIntent

    data object CreatePassword : OtpIntent

    data object CreateTotp : OtpIntent

    data object CloseCreateResourceMenu : OtpIntent

    // otp more menu
    data class OpenOtpMoreMenu(
        val otpItemWrapper: OtpItemWrapper,
    ) : OtpIntent

    data object CloseOtpMoreMenu : OtpIntent

    data class CopyOtp(
        val otpItemWrapper: OtpItemWrapper,
    ) : OtpIntent

    data class DeleteOtp(
        val otpItemWrapper: OtpItemWrapper,
    ) : OtpIntent

    data class EditOtp(
        val otpItemWrapper: OtpItemWrapper,
    ) : OtpIntent

    data object ConfirmDeleteTotp : OtpIntent

    data object CloseDeleteConfirmationDialog : OtpIntent

    // metadata keys
    data object CloseTrustNewKeyDialog : OtpIntent

    data object CloseTrustedKeyDeletedDialog : OtpIntent

    data object TrustMetadataKeyDeletion : OtpIntent

    data class TrustNewMetadataKey(
        val model: NewMetadataKeyToTrustModel,
    ) : OtpIntent
}
