package com.passbolt.mobile.android.core.security

import kotlin.math.ln

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
class PasswordGenerator {

    fun generate(
        alphabets: Set<Set<Char>> = CharacterSets.all,
        minLength: Int = DEFAULT_LENGTH,
        targetEntropy: Entropy = Entropy.VERY_STRONG
    ): String {
        require(minLength > 0 && targetEntropy.rawValue > 0)

        val stringBuilder = StringBuilder()
        val entireAlphabet = alphabets.flatten()

        while (!arePasswordCriteriaMet(stringBuilder, minLength, alphabets, targetEntropy)) {
            stringBuilder.append(entireAlphabet.random())
        }
        return stringBuilder.toString()
    }

    private fun arePasswordCriteriaMet(
        stringBuilder: StringBuilder,
        minLength: Int,
        alphabets: Set<Set<Char>>,
        targetEntropy: Entropy
    ) = stringBuilder.length >= minLength && Entropy.parse(
        getEntropy(
            stringBuilder.toString(),
            alphabets
        )
    ).rawValue >= targetEntropy.rawValue

    fun getEntropy(password: String, alphabets: Set<Set<Char>> = CharacterSets.all): Double {
        if (password.isEmpty() || alphabets.isEmpty()) {
            return 0.0
        }
        val usedAlphabet = mutableSetOf<Char>()

        password.forEach { char ->
            val alphabet = alphabets.find { it.contains(char) }.orEmpty()
            usedAlphabet.addAll(alphabet)
        }

        return password.length.toDouble() * (ln(usedAlphabet.size.toDouble()) / ln(2.0))
    }

    @Suppress("MagicNumber")
    enum class Entropy(val rawValue: Int) {
        ZERO(0),
        VERY_WEAK(1),
        WEAK(60),
        FAIR(80),
        STRONG(112),
        VERY_STRONG(128),
        GREATEST_FINITE(Int.MAX_VALUE);

        companion object {
            fun parse(value: Double): Entropy = when (value) {
                in 0.0..1.0 -> ZERO
                in 1.0..60.0 -> VERY_WEAK
                in 60.0..80.0 -> WEAK
                in 80.0..112.0 -> FAIR
                in 112.0..128.0 -> STRONG
                in 128.0..Int.MAX_VALUE.toDouble() -> VERY_STRONG
                else -> GREATEST_FINITE
            }
        }
    }

    companion object {
        private const val DEFAULT_LENGTH = 18
    }
}
