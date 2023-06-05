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

package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.validation

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.testParserModule
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject


class SecretValidationTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testParserModule)
    }

    private val secretValidationRunner: SecretValidationRunner by inject()

    @Test
    fun `invalid secret for password string resource type should be rejected`() {
        val tooLongPassword = (0..PasswordStringSecretValidation.PASSWORD_STRING_PASSWORD_MAX_LENGTH + 1)
            .joinToString { "a" }

        val result = secretValidationRunner.isPasswordStringSecretValid(DecryptedSecret.SimplePassword(tooLongPassword))

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid secret for password and description resource type should be rejected`() {
        val tooLongPassword =
            (0..PasswordAndDescriptionSecretValidation.PASSWORD_AND_DESCRIPTION_PASSWORD_MAX_LENGTH + 1)
                .joinToString { "a" }
        val tooLongDescription =
            (0..PasswordAndDescriptionSecretValidation.PASSWORD_AND_DESCRIPTION_DESCRIPTION_MAX_LENGTH + 1)
                .joinToString { "a" }
        val invalidSecrets = listOf(
            DecryptedSecret.PasswordWithDescription(
                "desc",
                tooLongPassword
            ), DecryptedSecret.PasswordWithDescription(
                tooLongDescription,
                "pass"
            )
        )

        val results = invalidSecrets.map { secretValidationRunner.isPasswordAndDescriptionSecretValid(it) }

        assertThat(results.none { it }).isTrue()
    }

    @Test
    fun `invalid secret for totp resource type should be rejected`() {
        val invalidAlgorithm = "SHA2"
        val tooLongKey = (0..TotpSecretValidation.TOTP_KEY_MAX_LENGTH + 1)
            .joinToString { "a" }
        val tooFewDigits = TotpSecretValidation.TOTP_DIGITS_INCLUSIVE_MIN - 1
        val tooManyDigits = TotpSecretValidation.TOTP_DIGITS_INCLUSIVE_MAX + 1

        val invalidSecrets = listOf(
            DecryptedSecret.StandaloneTotp(DecryptedSecret.StandaloneTotp.Totp(invalidAlgorithm, "A", 6, 1)),
            DecryptedSecret.StandaloneTotp(DecryptedSecret.StandaloneTotp.Totp("SHA1", tooLongKey, 6, 1)),
            DecryptedSecret.StandaloneTotp(DecryptedSecret.StandaloneTotp.Totp("SHA1", "A", tooFewDigits, 1)),
            DecryptedSecret.StandaloneTotp(DecryptedSecret.StandaloneTotp.Totp("SHA1", "A", tooManyDigits, 1)),
        )

        val results = invalidSecrets.map { secretValidationRunner.isTotpSecretValid(it) }

        assertThat(results.none { it }).isTrue()
    }
}
