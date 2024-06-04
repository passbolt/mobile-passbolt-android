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

package com.passbolt.mobile.android.core.policies.validation

import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordPolicies

class PasswordPoliciesValidator {

    fun arePasswordPoliciesValid(passwordPolicies: PasswordPolicies) =
        isPasswordLengthValid(passwordPolicies.passwordGeneratorSettings.length) &&
                isAtLeasOnePasswordMaskSet(passwordPolicies.passwordGeneratorSettings) &&
                isPassphraseWordCountValid(passwordPolicies.passphraseGeneratorSettings)

    private fun isPassphraseWordCountValid(passphraseGeneratorSettings: PassphraseGeneratorSettingsModel) =
        passphraseGeneratorSettings.words in PASSPHRASE_GEN_MIN_WORDS..PASSPHRASE_GEN_MAX_WORDS

    private fun isAtLeasOnePasswordMaskSet(passwordGeneratorSettingsModel: PasswordGeneratorSettingsModel) =
        listOf(
            passwordGeneratorSettingsModel.maskChar1,
            passwordGeneratorSettingsModel.maskChar2,
            passwordGeneratorSettingsModel.maskChar3,
            passwordGeneratorSettingsModel.maskChar4,
            passwordGeneratorSettingsModel.maskChar5,
            passwordGeneratorSettingsModel.maskEmoji,
            passwordGeneratorSettingsModel.maskDigit,
            passwordGeneratorSettingsModel.maskParenthesis,
            passwordGeneratorSettingsModel.maskLower,
            passwordGeneratorSettingsModel.maskUpper
        ).any()

    private fun isPasswordLengthValid(passwordLength: Int) =
        passwordLength in PASSWORD_GEN_MIN_PASSWORD_LENGTH..PASSWORD_GEN_MAX_PASSWORD_LENGTH

    private companion object {
        const val PASSWORD_GEN_MIN_PASSWORD_LENGTH = 8
        const val PASSWORD_GEN_MAX_PASSWORD_LENGTH = 128
        const val PASSPHRASE_GEN_MIN_WORDS = 4
        const val PASSPHRASE_GEN_MAX_WORDS = 40
    }
}
