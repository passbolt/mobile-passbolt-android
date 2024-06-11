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
import com.passbolt.mobile.android.core.passwordgenerator.passwordGeneratorModule
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject


class EntropyCalculatorTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(passwordGeneratorModule)
    }

    private val entropyCalculator: EntropyCalculator by inject()


    @Test
    fun `test entropy for short string generated from short character set`() {
        val alphabetSet = setOf("A", "B", "C").map { CodepointSet("label", "name", it.toCodepoints()) }.toSet()
        val result = entropyCalculator.getEntropy("ABC".toCodepoints(), alphabetSet)
        assertEquals(result, 4.75, 0.1)
    }

    @Test
    fun `test entropy for short string generated from all characters`() {
        val alphabetSet = Alphabets.all.values.toSet()
        val result = entropyCalculator.getEntropy("ABC".toCodepoints(), alphabetSet)

        assertEquals(result, 14.1, 0.1)
    }

    @Test
    fun `test entropy for long string with all available characters`() {
        val alphabetSet = Alphabets.all.values.toSet()
        val result = entropyCalculator.getEntropy(
            "###\"@L./Jfc^J&7{1cIs_W172Bir5qm\"b:Lkd%3oY.\\!]X#j(gi;B<Y\"'SWOPX')_KGMZO.[/3:P!ibyJa?x\$gN#\$dT~QOXF?.y9^AH?[teQDbkGsBTs[-ZQ8au/~@+ag\$uFJ9D72uew?i!q!*J01[w:``_g\"###".toCodepoints(),
            alphabetSet
        )

        assertEquals(result, 1046.0, 1.0)
    }

    @Test
    fun `test entropy for string only unknown characters should not be undefined`() {
        val alphabetSet =
            (koreanCharacters + koreanDigits).map { CodepointSet("label", "name", it.toCodepoints()) }.toSet()
        val result = entropyCalculator.getEntropy("ㄸㅉ삼공육ㄴㅌ오ㅡㅎㅁㅣ이ㄲ륙삼ㅁㅇㅋ육ㄱ팔삼".toCodepoints(), alphabetSet)
        assertThat(result).isGreaterThan(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun `test entropy for string with mixed unknown and known characters should be greater than with only known characters`() {
        val alphabetSet = Alphabets.all.values.toSet()

        val resultWithKnown = entropyCalculator.getEntropy("ABC".toCodepoints(), alphabetSet)
        val resultWithUnknown = entropyCalculator.getEntropy("ABC일".toCodepoints(), alphabetSet)

        assertThat(resultWithUnknown).isGreaterThan(resultWithKnown)
    }

    @Test
    fun `adding unknown characters should increase entropy`() {
        val alphabetSet = Alphabets.all.values.toSet()

        val result1 = entropyCalculator.getEntropy("ABC일".toCodepoints(), alphabetSet)
        val result2 = entropyCalculator.getEntropy("ABC일일".toCodepoints(), alphabetSet)

        assertThat(result2).isGreaterThan(result1)
    }

    private val koreanCharacters: Set<String> = setOf(
        "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ",
        "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ",
        "ㅈ", "ㅉ", "ㅎ", "ㅊ", "ㅋ", "ㅌ",
        "ㅍ", "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ",
        "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅛ", "ㅜ",
        "ㅠ", "ㅘ", "ㅙ", "ㅚ", "ㅝ", "ㅞ",
        "ㅟ", "ㅡ", "ㅢ", "ㅣ"
    )

    // Hangul digits - https://en.wikipedia.org/wiki/Korean_numerals
    private val koreanDigits: Set<String> = setOf(
        "영", "령", "공",  // 0
        "일",
        "이",
        "삼",
        "사",
        "오",
        "육", "륙",  // 6
        "칠",
        "팔",
        "구",
        "십",  // 10
    )
}
