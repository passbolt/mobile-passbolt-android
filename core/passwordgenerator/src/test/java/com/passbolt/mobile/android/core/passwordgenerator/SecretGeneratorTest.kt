package com.passbolt.mobile.android.core.passwordgenerator

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy
import com.passbolt.mobile.android.ui.CaseTypeModel
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
class SecretGeneratorTest : KoinTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(passwordGeneratorTestModule)
        }

    private val secretGenerator: SecretGenerator by inject()

    @Test
    fun `generate should return low entropy failure for low password settings`() =
        runTest {
            val length = 3
            val settings =
                PasswordGeneratorSettingsModel(
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
                    excludeLookAlikeChars = false,
                )

            val passwordGenerationResult = secretGenerator.generatePassword(settings)

            assertThat(passwordGenerationResult).isInstanceOf(FailedToGenerateLowEntropy::class.java)
        }

    @Test
    fun `generate should return low entropy failure for low passphrase settings`() =
        runTest {
            val settings =
                PassphraseGeneratorSettingsModel(
                    words = 1,
                    wordSeparator = "",
                    wordCase = CaseTypeModel.LOWERCASE,
                )

            val passphraseGenerationResult = secretGenerator.generatePassphrase(settings)

            assertThat(passphraseGenerationResult).isInstanceOf(FailedToGenerateLowEntropy::class.java)
        }
}
