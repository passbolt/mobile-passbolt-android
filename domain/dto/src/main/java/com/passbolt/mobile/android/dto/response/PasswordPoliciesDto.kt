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

package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class PasswordPoliciesDto(
    val id: UUID,
    @SerializedName("default_generator")
    val defaultGenerator: PasswordGeneratorType,
    @SerializedName("password_generator_settings")
    val passwordGeneratorSettings: PasswordGeneratorSettings,
    @SerializedName("passphrase_generator_settings")
    val passphraseGeneratorSettings: PassphraseGeneratorSettings,
    @SerializedName("external_dictionary_check")
    val externalDictionaryCheck: Boolean
)

data class PasswordGeneratorSettings(
    val length: Int,
    @SerializedName("mask_upper")
    val maskUpper: Boolean,
    @SerializedName("mask_lower")
    val maskLower: Boolean,
    @SerializedName("mask_digit")
    val maskDigit: Boolean,
    @SerializedName("mask_parenthesis")
    val maskParenthesis: Boolean,
    @SerializedName("mask_emoji")
    val maskEmoji: Boolean,
    @SerializedName("mask_char1")
    val maskChar1: Boolean,
    @SerializedName("mask_char2")
    val maskChar2: Boolean,
    @SerializedName("mask_char3")
    val maskChar3: Boolean,
    @SerializedName("mask_char4")
    val maskChar4: Boolean,
    @SerializedName("mask_char5")
    val maskChar5: Boolean,
    @SerializedName("exclude_look_alike_chars")
    val excludeLookAlikeChars: Boolean
)

data class PassphraseGeneratorSettings(
    val words: Int,
    @SerializedName("word_separator")
    val wordSeparator: String,
    @SerializedName("word_case")
    val wordCase: CaseType
)

enum class PasswordGeneratorType {
    @SerializedName("password")
    PASSWORD,

    @SerializedName("passphrase")
    PASSPHRASE
}

enum class CaseType {
    @SerializedName("uppercase")
    UPPERCASE,

    @SerializedName("lowercase")
    LOWERCASE,

    @SerializedName("camelcase")
    CAMELCASE
}
