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

package com.passbolt.mobile.android.core.passwordgenerator.entropy

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.Alphabets
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.CodepointSet
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.core.passwordgenerator.dice.Dice
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext
import kotlin.math.ln

class EntropyCalculator(
    private val dice: Dice,
    private val coroutineLaunchContext: CoroutineLaunchContext
) {
    suspend fun getPasswordEntropy(
        password: List<Codepoint>,
        alphabets: Set<CodepointSet>
    ): Double =
        if (password.isEmpty() || alphabets.isEmpty()) {
            0.0
        } else {
            withContext(coroutineLaunchContext.io) {
                val usedKnownAlphabet = mutableSetOf<Codepoint>()
                val usedUnknownAlphabet = mutableSetOf<Codepoint>()

                password.forEach { codepoint ->
                    val alphabet = alphabets.find { it.codepoints.contains(codepoint) }?.codepoints ?: emptyList()
                    if (alphabet.isNotEmpty()) {
                        usedKnownAlphabet.addAll(alphabet)
                    } else {
                        usedUnknownAlphabet.add(codepoint)
                    }
                }

                if (usedKnownAlphabet.isEmpty()) {
                    // when all characters are unknown return undefined entropy
                    Double.NEGATIVE_INFINITY
                } else {
                    password.size.toDouble() * (
                            ln(usedKnownAlphabet.size.toDouble() + usedUnknownAlphabet.size.toDouble()) /
                                    ln(2.0)
                            )
                }
            }
        }

    suspend fun getPassphraseEntropy(passphraseWordCount: Int, separator: String): Double =
        withContext(coroutineLaunchContext.io) {
            dice.apply {
                initialize()
                isInitializedFlow.takeWhile { !it }
            }
            passphraseWordCount.toDouble() * (
                    ln(dice.dictionarySize.toDouble() * WORD_CASE_NUMBER) /
                            ln(2.0)
                    ) +
                    getPasswordEntropy(separator.toCodepoints(), Alphabets.all.values.toSet())
        }

    suspend fun getSecretEntropy(secret: String): Double {
        return withContext(coroutineLaunchContext.io) {
            var diceWordsCount = 0
            var secretCopy = secret
            dice.apply {
                initialize()
                isInitializedFlow.takeWhile { !it }
            }

            dice.getDescendingLengthSortedWords().forEach { word ->
                if (secretCopy.contains(word)) {
                    secretCopy = secretCopy.replace(word, "")
                    diceWordsCount++
                }
            }

            if (diceWordsCount < 2) {
                getPasswordEntropy(secret.toCodepoints(), Alphabets.all.values.toSet())
            } else {
                try {
                    val separatorCount = diceWordsCount - 1
                    val separators = secretCopy.chunked(secretCopy.length / separatorCount)

                    require(separators.all { it == separators.first() })

                    getPassphraseEntropy(diceWordsCount, separators.first())
                } catch (exception: Exception) {
                    getPasswordEntropy(secret.toCodepoints(), Alphabets.all.values.toSet())
                }
            }
        }
    }

    private companion object {
        private const val WORD_CASE_NUMBER = 3 // upper case, lower case, camel case
    }
}
