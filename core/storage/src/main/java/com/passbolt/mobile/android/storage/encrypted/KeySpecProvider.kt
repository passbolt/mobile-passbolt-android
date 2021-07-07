package com.passbolt.mobile.android.storage.encrypted

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.MasterKey
import com.passbolt.mobile.android.storage.usecase.KEY_SIZE

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

internal class KeySpecProvider {

    fun get(keyBiometricSettings: KeyBiometricSettings): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(keyBiometricSettings.authenticationRequired)
            .setInvalidatedByBiometricEnrollment(keyBiometricSettings.invalidatedByBiometricEnrollment)
            .setKeySize(KEY_SIZE)

        if (keyBiometricSettings.authenticationRequired) {
            builder.setAuthParameters()
        }

        return builder.build()
    }

    private fun KeyGenParameterSpec.Builder.setAuthParameters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setUserAuthenticationParameters(AUTH_TIMEOUT_SECONDS, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            setUserAuthenticationValidityDurationSeconds(AUTH_TIMEOUT_SECONDS)
        }
    }

    companion object {
        private const val AUTH_TIMEOUT_SECONDS = 30
    }
}

class KeyBiometricSettings(
    val authenticationRequired: Boolean,
    val invalidatedByBiometricEnrollment: Boolean
)
