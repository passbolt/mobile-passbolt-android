package com.passbolt.mobile.android.core.passwordgenerator.codepoints

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
data class CodepointSet(
    val label: String,
    val name: String,
    val codepoints: List<Codepoint>
) {
    companion object {

        fun CodepointSet.withLookAlikeExcluded(excludeLookAlike: Boolean): CodepointSet = if (excludeLookAlike) {
            this.copy(codepoints = this.codepoints.filter { codepoint ->
                !lookAlikeCodepoints.contains(codepoint.value)
            })
        } else {
            this
        }
    }
}

data class Codepoint(val value: Int) {

    val characterCount: Int
        get() = Character.charCount(value)
}

fun String.toCodepoints(): List<Codepoint> {
    val result = mutableListOf<Codepoint>()

    var offset = 0
    while (offset < length) {
        val codepoint = Codepoint(codePointAt(offset))
        result.add(codepoint)
        offset += codepoint.characterCount
    }
    return result
}

private val lookAlikeCodepoints = listOf(
    "O", "l", "|", "I", "0", "1"
).map { it.codePointAt(0) }
