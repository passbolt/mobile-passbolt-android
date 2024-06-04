package com.passbolt.mobile.android.core.passwordgenerator

import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.assertEquals
import org.junit.Test

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
class PasswordGeneratorTest {

    private val passwordGenerator = com.passbolt.mobile.android.core.passwordgenerator.PasswordGenerator()

    @Test
    fun `generate short string succeeds`() {
        val alphabetSet = setOf('A', 'B', 'C')
        val result = passwordGenerator.generate(setOf(alphabetSet), 18, com.passbolt.mobile.android.core.passwordgenerator.PasswordGenerator.Entropy.FAIR)

        assertThat(result.length >= 18).isTrue()
    }

    @Test
    fun `generate string with latin and korean characters and digits succeeds`() {
        val alphabetSet = setOf(
            CharacterSets.lowercaseLetters,
            CharacterSets.uppercaseLetters,
            CharacterSets.digits,
            koreanCharacters,
            CharacterSets.digits
        )
        val result = passwordGenerator.generate(alphabetSet, 18, com.passbolt.mobile.android.core.passwordgenerator.PasswordGenerator.Entropy.VERY_STRONG)

        assertThat(result.length >= 18).isTrue()
    }

    @Test
    fun `test entropy for short string generated from short character set`() {
        val alphabetSet = setOf('A', 'B', 'C')
        val result = passwordGenerator.getEntropy("ABC", setOf(alphabetSet))
        assertEquals(result, 4.75, 0.1)
    }

    @Test
    fun `test entropy for short string generated from all characters`() {
        val alphabetSet = com.passbolt.mobile.android.core.passwordgenerator.CharacterSets.all
        val result = passwordGenerator.getEntropy("ABC", alphabetSet)
        assertEquals(result, 14.1, 0.1)
    }

    @Test
    fun `test entropy for longer alphanumeric string`() {
        val alphabetSet = com.passbolt.mobile.android.core.passwordgenerator.CharacterSets.alphanumeric
        val result = passwordGenerator.getEntropy("oIabpwLaCaTYE3yOZheQ", alphabetSet)
        assertEquals(result, 119.0, 0.1)
    }

    @Test
    fun `test entropy for long string with all available characters`() {
        val alphabetSet = com.passbolt.mobile.android.core.passwordgenerator.CharacterSets.all
        val result = passwordGenerator.getEntropy("###\"@L./Jfc^J&7{1cIs_W172Bir5qm\"b:Lkd%3oY.\\!]X#j(gi;B<Y\"'SWOPX')_KGMZO.[/3:P!ibyJa?x\$gN#\$dT~QOXF?.y9^AH?[teQDbkGsBTs[-ZQ8au/~@+ag\$uFJ9D72uew?i!q!*J01[w:``_g\"###", alphabetSet)
        assertEquals(result, 1046.0, 1.0)
    }

    @Test
    fun `test entropy for string with korean characters and digits`() {
        val alphabetSet = setOf(koreanCharacters, koreanDigits)
        val result = passwordGenerator.getEntropy("ㄸㅉ삼공육ㄴㅌ오ㅡㅎㅁㅣ이ㄲ륙삼ㅁㅇㅋ육ㄱ팔삼", alphabetSet)
        assertEquals(result, 132.36, 0.1)
    }

    @Test
    fun `test entropy for string with latin alphabet and additional korean characters`() {
        val alphabetSet = com.passbolt.mobile.android.core.passwordgenerator.CharacterSets.all
        val result = passwordGenerator.getEntropy("2!wㅎl;piwㅝWQca]영", alphabetSet)
        assertEquals(result, 105.0, 1.0)
    }

    private val koreanCharacters: Set<Char> = setOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ',
        'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ',
        'ㅈ', 'ㅉ', 'ㅎ', 'ㅊ', 'ㅋ', 'ㅌ',
        'ㅍ', 'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ',
        'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅛ', 'ㅜ',
        'ㅠ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅝ', 'ㅞ',
        'ㅟ', 'ㅡ', 'ㅢ', 'ㅣ'
    )

    // Hangul digits - https://en.wikipedia.org/wiki/Korean_numerals
    private val koreanDigits: Set<Char> = setOf(
        '영', '령', '공',  // 0
        '일',
        '이',
        '삼',
        '사',
        '오',
        '육', '륙',  // 6
        '칠',
        '팔',
        '구',
        '십',  // 10
    )
}
