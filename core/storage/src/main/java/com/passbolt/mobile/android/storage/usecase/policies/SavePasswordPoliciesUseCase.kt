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

package com.passbolt.mobile.android.storage.usecase.policies

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.storage.encrypted.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_DEFAULT_GENERATOR
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_EXTERNAL_DICTIONARY_CHECK_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSPHRASE_GENERATOR_CASE
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSPHRASE_GENERATOR_SEPARATOR
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSPHRASE_GENERATOR_WORDS_COUNT
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_EXCLUDE_LOOK_ALIKE_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_LENGTH
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_CHAR1_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_CHAR2_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_CHAR3_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_CHAR4_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_CHAR5_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_DIGIT_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_EMOJI_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_LOWER_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_PARENTHESIS_ENABLED
import com.passbolt.mobile.android.storage.paths.PasswordPoliciesFileName.Companion.KEY_PASSWORD_GENERATOR_MASK_UPPER_ENABLED
import com.passbolt.mobile.android.storage.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.ui.PasswordPolicies

class SavePasswordPoliciesUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : AsyncUseCase<SavePasswordPoliciesUseCase.Input, Unit>, SelectedAccountUseCase {

    @Suppress("LongMethod")
    override suspend fun execute(input: Input) {
        val fileName = PasswordPoliciesFileName(selectedAccountId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")
        with(sharedPreferences.edit()) {
            putInt(KEY_DEFAULT_GENERATOR, input.passwordExpirySettings.defaultGenerator.ordinal)
            putInt(KEY_PASSWORD_GENERATOR_LENGTH, input.passwordExpirySettings.passwordGeneratorSettings.length)
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_UPPER_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskUpper
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_LOWER_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskLower
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_DIGIT_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskDigit
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_PARENTHESIS_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskParenthesis
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_EMOJI_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskEmoji
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_CHAR1_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskChar1
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_CHAR2_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskChar2
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_CHAR3_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskChar3
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_CHAR4_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskChar4
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_MASK_CHAR5_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.maskChar5
            )
            putBoolean(
                KEY_PASSWORD_GENERATOR_EXCLUDE_LOOK_ALIKE_ENABLED,
                input.passwordExpirySettings.passwordGeneratorSettings.excludeLookAlikeChars
            )
            putInt(KEY_PASSPHRASE_GENERATOR_WORDS_COUNT, input.passwordExpirySettings.passphraseGeneratorSettings.words)
            putString(
                KEY_PASSPHRASE_GENERATOR_SEPARATOR,
                input.passwordExpirySettings.passphraseGeneratorSettings.wordSeparator
            )
            putInt(
                KEY_PASSPHRASE_GENERATOR_CASE,
                input.passwordExpirySettings.passphraseGeneratorSettings.wordCase.ordinal
            )
            putBoolean(
                KEY_EXTERNAL_DICTIONARY_CHECK_ENABLED,
                input.passwordExpirySettings.isExternalDictionaryCheckEnabled
            )
            apply()
        }
    }

    data class Input(val passwordExpirySettings: PasswordPolicies)
}
