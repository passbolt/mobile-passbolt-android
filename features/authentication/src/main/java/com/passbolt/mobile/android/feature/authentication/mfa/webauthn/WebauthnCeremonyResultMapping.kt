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

import android.app.Activity
import androidx.browser.auth.AuthTabIntent
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Completed
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed

private const val QUERY_EXCHANGE_CODE = "exchange_code"
private const val QUERY_ERROR = "error"
private const val ERROR_USER_CANCELLED = "user_cancelled"

internal fun AuthTabIntent.AuthResult.toWebauthnCeremonyResult(): WebauthnCeremonyResult {
    if (resultCode == Activity.RESULT_CANCELED) {
        return Cancelled
    }
    if (resultCode != Activity.RESULT_OK) {
        return Failed("auth_tab_result_$resultCode")
    }
    val exchangeCode = resultUri?.getQueryParameter(QUERY_EXCHANGE_CODE)
    val error = resultUri?.getQueryParameter(QUERY_ERROR)
    return when {
        !exchangeCode.isNullOrBlank() -> Completed(exchangeCode)
        error == ERROR_USER_CANCELLED -> Cancelled
        else -> Failed(error)
    }
}
