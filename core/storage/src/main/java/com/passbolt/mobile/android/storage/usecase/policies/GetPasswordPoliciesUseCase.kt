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
import com.passbolt.mobile.android.ui.CaseTypeModel
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.PasswordPolicies

class GetPasswordPoliciesUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : AsyncUseCase<Unit, PasswordPolicies>, SelectedAccountUseCase {

    @Suppress("LongMethod")
    override suspend fun execute(input: Unit): PasswordPolicies {
        val fileName = PasswordPoliciesFileName(selectedAccountId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")

        val defaultGenerator = PasswordGeneratorTypeModel.entries[
            sharedPreferences.getInt(KEY_DEFAULT_GENERATOR, DEFAULT_PASS_GENERATOR_TYPE_ORDINAL)
        ]
        val passwordGeneratorLength = sharedPreferences.getInt(
            KEY_PASSWORD_GENERATOR_LENGTH, DEFAULT_PASSWORD_GENERATOR_LENGTH
        )
        val passwordGeneratorMaskUpper = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_UPPER_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_UPPER_ENABLED
        )
        val passwordGeneratorMaskLower = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_LOWER_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_LOWER_ENABLED
        )
        val passwordGeneratorMaskDigit = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_DIGIT_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_DIGIT_ENABLED
        )
        val passwordGeneratorMaskParenthesis =
            sharedPreferences.getBoolean(
                KEY_PASSWORD_GENERATOR_MASK_PARENTHESIS_ENABLED,
                DEFAULT_PASSWORD_GENERATOR_MASK_PARENTHESIS_ENABLED
            )
        val passwordGeneratorMaskEmoji = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_EMOJI_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_EMOJI_ENABLED
        )
        val passwordGeneratorMaskChar1 = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_CHAR1_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_CHAR1_ENABLED
        )
        val passwordGeneratorMaskChar2 = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_CHAR2_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_CHAR2_ENABLED
        )
        val passwordGeneratorMaskChar3 = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_CHAR3_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_CHAR3_ENABLED
        )
        val passwordGeneratorMaskChar4 = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_CHAR4_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_CHAR4_ENABLED
        )
        val passwordGeneratorMaskChar5 = sharedPreferences.getBoolean(
            KEY_PASSWORD_GENERATOR_MASK_CHAR5_ENABLED,
            DEFAULT_PASSWORD_GENERATOR_MASK_CHAR5_ENABLED
        )
        val passwordGeneratorExcludeLookAlikeChars =
            sharedPreferences.getBoolean(
                KEY_PASSWORD_GENERATOR_EXCLUDE_LOOK_ALIKE_ENABLED,
                DEFAULT_PASSWORD_GENERATOR_EXCLUDE_LOOK_ALIKE_ENABLED
            )
        val passphraseGeneratorWordsCount =
            sharedPreferences.getInt(KEY_PASSPHRASE_GENERATOR_WORDS_COUNT, DEFAULT_PASSPHRASE_GENERATOR_WORDS_COUNT)
        val passphraseGeneratorSeparator =
            sharedPreferences.getString(KEY_PASSPHRASE_GENERATOR_SEPARATOR, DEFAULT_PASSPHRASE_GENERATOR_SEPARATOR)
                ?: DEFAULT_PASSPHRASE_GENERATOR_SEPARATOR
        val passphraseGeneratorCase = CaseTypeModel.entries[sharedPreferences.getInt(
            KEY_PASSPHRASE_GENERATOR_CASE,
            DEFAULT_PASSPHRASE_GENERATOR_CASE_TYPE_ORDINAL
        )]
        val externalDictionaryCheckEnabled = sharedPreferences.getBoolean(
            KEY_EXTERNAL_DICTIONARY_CHECK_ENABLED,
            DEFAULT_EXTERNAL_DICTIONARY_CHECK_ENABLED
        )

        return PasswordPolicies(
            defaultGenerator,
            PasswordGeneratorSettingsModel(
                length = passwordGeneratorLength,
                maskUpper = passwordGeneratorMaskUpper,
                maskLower = passwordGeneratorMaskLower,
                maskDigit = passwordGeneratorMaskDigit,
                maskParenthesis = passwordGeneratorMaskParenthesis,
                maskEmoji = passwordGeneratorMaskEmoji,
                maskChar1 = passwordGeneratorMaskChar1,
                maskChar2 = passwordGeneratorMaskChar2,
                maskChar3 = passwordGeneratorMaskChar3,
                maskChar4 = passwordGeneratorMaskChar4,
                maskChar5 = passwordGeneratorMaskChar5,
                excludeLookAlikeChars = passwordGeneratorExcludeLookAlikeChars
            ),
            PassphraseGeneratorSettingsModel(
                words = passphraseGeneratorWordsCount,
                wordSeparator = passphraseGeneratorSeparator,
                wordCase = passphraseGeneratorCase
            ),
            isExternalDictionaryCheckEnabled = externalDictionaryCheckEnabled
        )
    }

    data class Output(val passwordPolicies: PasswordPolicies)

    private companion object {
        private const val DEFAULT_PASS_GENERATOR_TYPE_ORDINAL = 0
        private const val DEFAULT_PASSWORD_GENERATOR_LENGTH = 18
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_UPPER_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_LOWER_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_DIGIT_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_PARENTHESIS_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_EMOJI_ENABLED = false
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_CHAR1_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_CHAR2_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_CHAR3_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_CHAR4_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_MASK_CHAR5_ENABLED = true
        private const val DEFAULT_PASSWORD_GENERATOR_EXCLUDE_LOOK_ALIKE_ENABLED = true
        private const val DEFAULT_PASSPHRASE_GENERATOR_WORDS_COUNT = 9
        private const val DEFAULT_PASSPHRASE_GENERATOR_SEPARATOR = " "
        private const val DEFAULT_PASSPHRASE_GENERATOR_CASE_TYPE_ORDINAL = 0
        private const val DEFAULT_EXTERNAL_DICTIONARY_CHECK_ENABLED = true
    }
}
