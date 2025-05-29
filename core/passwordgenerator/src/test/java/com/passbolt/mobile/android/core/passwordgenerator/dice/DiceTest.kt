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

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.passwordGeneratorTestModule
import com.passbolt.mobile.android.ui.CaseTypeModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.Test

class DiceTest : KoinTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(passwordGeneratorTestModule)
        }

    private val dice: Dice by inject()

    @Test
    fun `dice file should be parsed correct`() =
        runTest {
            assertThat(dice.getWord(11111)).isEqualTo("abacus")
            assertThat(dice.getWord(66666)).isEqualTo("zoom")
        }

    @Test
    fun `invalid number should cause an exception`() =
        runTest {
            assertThrows(IllegalArgumentException::class.java) { dice.getWord(0) }
        }

    @Test
    fun `passphrase should be generated correctly with default separator`() =
        runTest {
            val wordsCount = 6
            val passphrase = dice.generatePassphrase(wordsCount, case = CaseTypeModel.LOWERCASE)

            val words = passphrase.split(Dice.DEFAULT_WORD_SEPARATOR)

            assertThat(words.size).isEqualTo(wordsCount)
            assertThat(words.all { it.isNotBlank() }).isTrue()
        }

    @Test
    fun `passphrase should be generated correctly with custom separator`() =
        runTest {
            val separator = ";"
            val wordsCount = 8
            val passphrase = dice.generatePassphrase(wordsCount, case = CaseTypeModel.LOWERCASE, wordsSeparator = separator)

            val words = passphrase.split(separator)

            assertThat(words.size).isEqualTo(wordsCount)
            assertThat(words.all { it.isNotBlank() }).isTrue()
        }

    @Test
    fun `passphrase should be generated correctly with camelcase`() =
        runTest {
            val separator = ";"
            val wordsCount = 8
            val passphrase = dice.generatePassphrase(wordsCount, case = CaseTypeModel.CAMELCASE, wordsSeparator = separator)

            val words = passphrase.split(separator).map { it.replace(separator, "") }

            assertThat(words.size).isEqualTo(wordsCount)
            assertThat(words.all { it.first().isUpperCase() }).isTrue()
        }

    @Test
    fun `passphrase should be generated correctly with uppercase`() =
        runTest {
            val separator = ";"
            val wordsCount = 8
            val passphrase = dice.generatePassphrase(wordsCount, case = CaseTypeModel.UPPERCASE, wordsSeparator = separator)

            val words = passphrase.split(separator).map { it.replace(separator, "") }

            assertThat(words.size).isEqualTo(wordsCount)
            assertThat(words.all { it.all { letter -> letter.isUpperCase() } }).isTrue()
        }
}
