package com.passbolt.mobile.android.feature.setup.fingerprint

import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.biometric.BiometricPrompt.ERROR_TIMEOUT
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import javax.crypto.Cipher
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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

class SetupBiometricCallback(
    private val authError: (Int) -> Unit,
    private val authSucceeded: (Cipher?) -> Unit,
) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence,
    ) {
        super.onAuthenticationError(errorCode, errString)
        handleError(errorCode)
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        authSucceeded.invoke(result.cryptoObject?.cipher)
    }

    private fun handleError(errorCode: Int) {
        when (errorCode) {
            ERROR_LOCKOUT -> {
                authError.invoke(LocalizationR.string.fingerprint_biometric_error_blocked)
            }
            ERROR_LOCKOUT_PERMANENT -> {
                authError.invoke(LocalizationR.string.fingerprint_biometric_error_too_many_attempts)
            }
            ERROR_NEGATIVE_BUTTON, ERROR_USER_CANCELED, ERROR_TIMEOUT -> {
                // ignoring
            }
            else -> {
                authError.invoke(LocalizationR.string.fingerprint_biometric_error_generic)
            }
        }
    }
}
