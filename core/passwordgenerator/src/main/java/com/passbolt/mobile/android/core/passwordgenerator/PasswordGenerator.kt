package com.passbolt.mobile.android.core.passwordgenerator

import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel

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
class PasswordGenerator(
    private val entropyCalculator: EntropyCalculator
) {

    fun generate(settings: PasswordGeneratorSettingsModel): PasswordGenerationResult {
        val codepointBuilder = mutableListOf<Codepoint>()
        val alphabets = Alphabets.getCodepointSetsForModel(settings)
        val entireAlphabet = alphabets.flatMap { it.codepoints }

        while (codepointBuilder.size < settings.length) {
            codepointBuilder.add(entireAlphabet.random())
        }

        val entropy = entropyCalculator.getEntropy(codepointBuilder, alphabets)
        return if (entropy < MIN_ENTROPY_BITS) {
            PasswordGenerationResult.FailedToGenerateStringEnoughPassword(MIN_ENTROPY_BITS)
        } else {
            PasswordGenerationResult.Success(codepointBuilder, entropy)
        }
    }

    sealed class PasswordGenerationResult {

        data class Success(
            val password: List<Codepoint>,
            val entropy: Double
        ) : PasswordGenerationResult()

        data class FailedToGenerateStringEnoughPassword(val minimumEntropyBits: Int) : PasswordGenerationResult()
    }

    private companion object {
        private const val MIN_ENTROPY_BITS = 80
    }
}
