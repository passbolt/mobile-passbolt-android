package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

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

sealed interface OtpNavigationKey : NavKey {
    @Serializable
    data object Otp : OtpNavigationKey

    @Serializable
    data class ScanOtp(
        val mode: ScanOtpMode,
        val parentFolderId: String? = null,
    ) : OtpNavigationKey

    @Serializable
    data class ScanOtpSuccess(
        val totpLabel: String,
        val totpSecret: String,
        val totpIssuer: String?,
        val totpAlgorithm: String,
        val totpDigits: Int,
        val totpPeriod: Long,
        val parentFolderId: String? = null,
    ) : OtpNavigationKey

    @Serializable
    data class ResourcePicker(
        val suggestionUri: String? = null,
    ) : OtpNavigationKey

    @Serializable
    enum class ScanOtpMode {
        SCAN_FOR_RESULT,
        SCAN_WITH_SUCCESS_SCREEN,
    }
}
