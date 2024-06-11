package com.passbolt.mobile.android.core.passwordgenerator

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.PasswordGenerator.PasswordGenerationResult
import com.passbolt.mobile.android.core.passwordgenerator.PasswordGenerator.PasswordGenerationResult.FailedToGenerateStringEnoughPassword
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.CodepointSet
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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
class PasswordGeneratorTest : KoinTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(passwordGeneratorModule)
    }

    private val passwordGenerator: PasswordGenerator by inject()

    @Test
    fun `generate should return low entropy failure for low settings`() {
        val length = 3
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = true,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        val passwordGenerationResult = passwordGenerator.generate(settings)

        assertThat(passwordGenerationResult).isInstanceOf(FailedToGenerateStringEnoughPassword::class.java)
    }

    @Test
    fun `generate upper case letters passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = true,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_UPPER]!!)
        )
    }

    @Test
    fun `generate lower case letters passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = true,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_LOWER]!!)
        )
    }

    @Test
    fun `generate digits passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = true,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_DIGIT]!!)
        )
    }

    @Test
    fun `generate parenthesis passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = true,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_PARENTHESIS]!!)
        )
    }

    @Test
    fun `generate character set 1 passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = true,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_SPECIAL_CHAR1]!!)
        )
    }

    @Test
    fun `generate character set 2 passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = true,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_SPECIAL_CHAR2]!!)
        )
    }

    @Test
    fun `generate character set 3 passwords succeeds`() {
        val length = 100
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = true,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_SPECIAL_CHAR3]!!)
        )
    }

    @Test
    fun `generate character set 4 passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = true,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_SPECIAL_CHAR4]!!)
        )
    }

    @Test
    fun `generate character set 5 passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = true,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_SPECIAL_CHAR5]!!)
        )
    }

    @Test
    fun `generate emoji passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = false,
            maskLower = false,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = true,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_EMOJI]!!)
        )
    }

    @Test
    fun `generate multi alphabet passwords succeeds`() {
        val length = 50
        val settings = PasswordGeneratorSettingsModel(
            length = length,
            maskUpper = true,
            maskLower = true,
            maskDigit = false,
            maskParenthesis = false,
            maskEmoji = false,
            maskChar1 = false,
            maskChar2 = false,
            maskChar3 = false,
            maskChar4 = false,
            maskChar5 = false,
            excludeLookAlikeChars = false
        )

        testPasswordAlphabetCorrectnessGeneration(
            settings,
            setOf(Alphabets.all[Alphabets.MASK_UPPER]!!, Alphabets.all[Alphabets.MASK_LOWER]!!)
        )
    }

    private fun testPasswordAlphabetCorrectnessGeneration(
        settings: PasswordGeneratorSettingsModel,
        alphabets: Set<CodepointSet>
    ) {
        val password = passwordGenerator.generate(settings)

        val codepoints = alphabets.flatMap { it.codepoints }
        assertPasswordGenerationIsSuccess(password)
        onPasswordGenerationSuccess(password) {
            assertThat(it.password.size).isEqualTo(settings.length)
            assertThat(it.password.all { codepoint -> codepoints.contains(codepoint) }).isTrue()
        }
    }

    private fun assertPasswordGenerationIsSuccess(passwordGenerationResult: PasswordGenerationResult) {
        assertThat(passwordGenerationResult).isInstanceOf(PasswordGenerationResult.Success::class.java)
    }

    private fun onPasswordGenerationSuccess(
        passwordGenerationResult: PasswordGenerationResult,
        block: (PasswordGenerationResult.Success) -> Unit
    ) {
        block(passwordGenerationResult as PasswordGenerationResult.Success)
    }
}
