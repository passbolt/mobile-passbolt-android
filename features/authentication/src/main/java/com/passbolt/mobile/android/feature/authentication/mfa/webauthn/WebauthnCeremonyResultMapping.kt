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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import androidx.browser.auth.AuthTabIntent
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed

internal fun AuthTabIntent.AuthResult.toWebauthnCeremonyResult(): WebauthnCeremonyResult =
    when (resultCode) {
        AuthTabIntent.RESULT_CANCELED -> Cancelled
        AuthTabIntent.RESULT_OK -> parseWebauthnCallbackUri(resultUri?.toString())
        AuthTabIntent.RESULT_VERIFICATION_FAILED -> Failed("auth_tab_verification_failed")
        AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> Failed("auth_tab_verification_timed_out")
        else -> Failed("auth_tab_unknown_result_$resultCode")
    }
