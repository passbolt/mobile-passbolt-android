package com.passbolt.mobile.android.core.passwordgenerator

import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.CodepointSet
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name 'Passbolt' is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to 'Passbolt' pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
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

object Alphabets {
    const val MASK_UPPER = "upper"
    const val MASK_LOWER = "lower"
    const val MASK_DIGIT = "digit"
    const val MASK_PARENTHESIS = "parenthesis"
    const val MASK_SPECIAL_CHAR1 = "special_char1"
    const val MASK_SPECIAL_CHAR2 = "special_char2"
    const val MASK_SPECIAL_CHAR3 = "special_char3"
    const val MASK_SPECIAL_CHAR4 = "special_char4"
    const val MASK_SPECIAL_CHAR5 = "special_char5"
    const val MASK_EMOJI = "emoji"

    val all = mapOf(
        MASK_UPPER to CodepointSet(
            label = "A-Z",
            name = MASK_UPPER,
            codepoints = listOf(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
            )
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_LOWER to CodepointSet(
            label = "a-z",
            name = MASK_LOWER,
            codepoints = listOf(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
                "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
            )
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_DIGIT to CodepointSet(
            label = "0-9",
            name = MASK_DIGIT,
            codepoints = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_PARENTHESIS to CodepointSet(
            label = "{ [ ( | ) ] ] }",
            name = MASK_PARENTHESIS,
            codepoints = listOf("{", "(", "[", "|", "]", ")", "}")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_SPECIAL_CHAR1 to CodepointSet(
            label = "# $ % & @ ^ ~",
            name = MASK_SPECIAL_CHAR1,
            codepoints = listOf("#", "$", "%", "&", "@", "^", "~")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_SPECIAL_CHAR2 to CodepointSet(
            label = ". , : ;",
            name = MASK_SPECIAL_CHAR2,
            codepoints = listOf(".", ",", ":", ";")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_SPECIAL_CHAR3 to CodepointSet(
            label = "' \" `",
            name = MASK_SPECIAL_CHAR3,
            codepoints = listOf("\'", "\"", "`")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_SPECIAL_CHAR4 to CodepointSet(
            label = "/ \\\\ _ -",
            name = MASK_SPECIAL_CHAR4,
            codepoints = listOf("/", "\\\\", "_", "-")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_SPECIAL_CHAR5 to CodepointSet(
            label = "< * + ! ? =",
            name = MASK_SPECIAL_CHAR5,
            codepoints = listOf("<", "*", "+", "!", "?", "=")
                .map { Codepoint(it.codePointAt(0)) }
        ),
        MASK_EMOJI to CodepointSet(
            label = "😘",
            name = MASK_EMOJI,
            codepoints = listOf(
                "😀", "😁", "😂", "😃", "😄", "😅", "😆", "😇", "😈", "😉",
                "😊", "😋", "😌", "😍", "😎", "😏", "😐", "😑", "😒", "😓",
                "😔", "😕", "😖", "😗", "😘", "😙", "😚", "😛", "😜", "😝",
                "😞", "😟", "😠", "😡", "😢", "😣", "😤", "😥", "😦", "😧",
                "😨", "😩", "😪", "😫", "😬", "😭", "😮", "😯", "😰", "😱",
                "😲", "😳", "😴", "😵", "😶", "😷", "😸", "😹", "😺", "😻",
                "😼", "😽", "😾", "😿", "🙀", "🙁", "🙂", "🙃", "🙄", "🙅",
                "🙆", "🙇", "🙈", "🙉", "🙊", "🙋", "🙌", "🙍", "🙎", "🙏"
            ).map { Codepoint(it.codePointAt(0)) }

        )
    )

    fun getCodepointSetsForModel(model: PasswordGeneratorSettingsModel): Set<CodepointSet> {
        val result = mutableSetOf<CodepointSet>()
        if (model.maskUpper) {
            result.add(all[MASK_UPPER]!!)
        }
        if (model.maskLower) {
            result.add(all[MASK_LOWER]!!)
        }
        if (model.maskDigit) {
            result.add(all[MASK_DIGIT]!!)
        }
        if (model.maskParenthesis) {
            result.add(all[MASK_PARENTHESIS]!!)
        }
        if (model.maskChar1) {
            result.add(all[MASK_SPECIAL_CHAR1]!!)
        }
        if (model.maskChar2) {
            result.add(all[MASK_SPECIAL_CHAR2]!!)
        }
        if (model.maskChar3) {
            result.add(all[MASK_SPECIAL_CHAR3]!!)
        }
        if (model.maskChar4) {
            result.add(all[MASK_SPECIAL_CHAR4]!!)
        }
        if (model.maskChar5) {
            result.add(all[MASK_SPECIAL_CHAR5]!!)
        }
        if (model.maskEmoji) {
            result.add(all[MASK_EMOJI]!!)
        }
        return result
    }
}
