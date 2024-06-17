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

package com.passbolt.mobile.android.core.passwordgenerator.dice

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.ui.CaseTypeModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.InputStream
import java.security.SecureRandom

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

class Dice(
    private val diceInputFileInputStream: InputStream,
    private val secureRandom: SecureRandom,
    private val coroutineLaunchContext: CoroutineLaunchContext
) {
    private val _isInitializedFlow = MutableStateFlow(false)
    val isInitializedFlow: Flow<Boolean> = _isInitializedFlow.asStateFlow()

    private val numbersToWords = hashMapOf<Int, String>()

    suspend fun initialize() {
        _isInitializedFlow.value = false
        withContext(coroutineLaunchContext.io) {
            diceInputFileInputStream.bufferedReader().lines().use { lines ->
                lines.forEach { line ->
                    val numberAndWord = line.split("\t")
                    numbersToWords[numberAndWord[0].toInt()] = numberAndWord[1]
                }
            }
        }
        _isInitializedFlow.value = true
    }

    @VisibleForTesting
    fun getWord(number: Int): String {
        return numbersToWords[number]
            ?: throw IllegalArgumentException("Number $number not found in the dice input file")
    }

    // https://www.eff.org/dice; use long words list
    suspend fun generatePassphrase(
        wordsCount: Int,
        case: CaseTypeModel,
        wordsSeparator: String = DEFAULT_WORD_SEPARATOR,
        diceCount: Int = DEFAULT_DICE_COUNT
    ): String {
        val result = mutableListOf<String>()
        withContext(coroutineLaunchContext.io) {
            repeat((1..wordsCount).count()) {
                val word =
                    (DICE_INDEX_SHIFT..diceCount)
                        .map { async { secureRandom.nextInt(DICE_MAX_VALUE) + DICE_INDEX_SHIFT } }
                        .awaitAll()
                        .joinToString(separator = "")
                        .toInt()
                        .let { getWord(it) }
                result.add(
                    when (case) {
                        CaseTypeModel.UPPERCASE -> word.uppercase()
                        CaseTypeModel.LOWERCASE -> word.lowercase()
                        CaseTypeModel.CAMELCASE -> word.replaceFirstChar { it.uppercase() }
                    }
                )
            }
        }
        return result.joinToString(separator = wordsSeparator)
    }

    companion object {
        private const val DICE_MAX_VALUE = 6
        private const val DICE_INDEX_SHIFT = 1

        @VisibleForTesting
        const val DEFAULT_DICE_COUNT = 5

        @VisibleForTesting
        const val DEFAULT_WORD_SEPARATOR = " "
    }
}
