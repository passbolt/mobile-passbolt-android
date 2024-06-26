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

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.Alphabets
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.CodepointSet
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.core.passwordgenerator.dice.Dice
import com.passbolt.mobile.android.core.passwordgenerator.passwordGeneratorTestModule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject


class EntropyCalculatorTest : KoinTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(passwordGeneratorTestModule)
    }

    private val entropyCalculator: EntropyCalculator by inject()

    @Test
    fun `test password entropy for short string generated from short character set`() = runTest {
        val alphabetSet = setOf("A", "B", "C").map { CodepointSet("label", "name", it.toCodepoints()) }.toSet()
        val result = entropyCalculator.getPasswordEntropy("ABC".toCodepoints(), alphabetSet)
        assertEquals(result, 4.75, 0.1)
    }

    @Test
    fun `test password entropy for short string generated from all characters`() = runTest {
        val alphabetSet = Alphabets.all.values.toSet()
        val result = entropyCalculator.getPasswordEntropy("ABC".toCodepoints(), alphabetSet)

        assertEquals(result, 14.1, 0.1)
    }

    @Test
    fun `test password entropy for long string with all available characters`() = runTest {
        val alphabetSet = Alphabets.all.values.toSet()
        val result = entropyCalculator.getPasswordEntropy(
            "###\"@L./Jfc^J&7{1cIs_W172Bir5qm\"b:Lkd%3oY.\\!]X#j(gi;B<Y\"'SWOPX')_KGMZO.[/3:P!ibyJa?x\$gN#\$dT~QOXF?.y9^AH?[teQDbkGsBTs[-ZQ8au/~@+ag\$uFJ9D72uew?i!q!*J01[w:``_g\"###".toCodepoints(),
            alphabetSet
        )

        assertEquals(result, 1046.0, 1.0)
    }

    @Test
    fun `test password entropy for string only unknown characters should be undefined`() = runTest {
        val alphabetSet = Alphabets.all.values.toSet()
        val result = entropyCalculator.getPasswordEntropy("ㄸㅉ삼공육ㄴㅌ오ㅡㅎㅁㅣ이ㄲ륙삼ㅁㅇㅋ육ㄱ팔삼".toCodepoints(), alphabetSet)
        assertThat(result).isEqualTo(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun `test password entropy for string with mixed unknown and known characters should be greater than with only known characters`() =
        runTest {
            val alphabetSet = Alphabets.all.values.toSet()

            val resultWithKnown = entropyCalculator.getPasswordEntropy("ABC".toCodepoints(), alphabetSet)
            val resultWithUnknown = entropyCalculator.getPasswordEntropy("ABC일".toCodepoints(), alphabetSet)

            assertThat(resultWithUnknown).isGreaterThan(resultWithKnown)
        }

    @Test
    fun `adding unknown characters to password should increase entropy`() = runTest {
        val alphabetSet = Alphabets.all.values.toSet()

        val result1 = entropyCalculator.getPasswordEntropy("ABC일".toCodepoints(), alphabetSet)
        val result2 = entropyCalculator.getPasswordEntropy("ABC일일".toCodepoints(), alphabetSet)

        assertThat(result2).isGreaterThan(result1)
    }

    @Test
    fun `test passphrase entropy for sample data`() = runTest {
        val result = entropyCalculator.getPassphraseEntropy(9, "; ")

        assertEquals(result, 135.23, 0.01)
    }

    @Test
    fun `test passphrase entropy with no separator`() = runTest {
        val result = entropyCalculator.getPassphraseEntropy(9, "")

        assertEquals(result, 130.59, 0.01)
    }

    @Test
    fun `test passphrase entropy with only unknown chars separator should not be undefined`() = runTest {
        val result = entropyCalculator.getPassphraseEntropy(9, " ")

        assertThat(result).isGreaterThan(0.0)
    }
}
