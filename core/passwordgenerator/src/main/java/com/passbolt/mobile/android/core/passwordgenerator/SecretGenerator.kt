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

package com.passbolt.mobile.android.core.passwordgenerator

import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel

class SecretGenerator(
    private val passwordGenerator: PasswordGenerator,
    private val passphraseGenerator: PassphraseGenerator,
    private val entropyCalculator: EntropyCalculator,
) {
    suspend fun generatePassword(settings: PasswordGeneratorSettingsModel): SecretGenerationResult {
        val password = passwordGenerator.generate(settings)
        val entropy = entropyCalculator.getPasswordEntropy(password, Alphabets.getCodepointSetsForModel(settings))

        return returnResult(password, entropy)
    }

    suspend fun generatePassphrase(settings: PassphraseGeneratorSettingsModel): SecretGenerationResult {
        val passphrase = passphraseGenerator.generate(settings)
        val entropy =
            entropyCalculator.getPassphraseEntropy(
                settings.words,
                settings.wordSeparator,
            )

        return returnResult(passphrase, entropy)
    }

    private fun returnResult(
        secret: List<Codepoint>,
        entropy: Double,
    ): SecretGenerationResult =
        if (entropy < MIN_ENTROPY_BITS) {
            SecretGenerationResult.FailedToGenerateLowEntropy(MIN_ENTROPY_BITS)
        } else {
            SecretGenerationResult.Success(secret, entropy)
        }

    sealed class SecretGenerationResult {
        data class Success(
            val password: List<Codepoint>,
            val entropy: Double,
        ) : SecretGenerationResult()

        data class FailedToGenerateLowEntropy(
            val minimumEntropyBits: Int,
        ) : SecretGenerationResult()
    }

    private companion object {
        private const val MIN_ENTROPY_BITS = 80
    }
}
