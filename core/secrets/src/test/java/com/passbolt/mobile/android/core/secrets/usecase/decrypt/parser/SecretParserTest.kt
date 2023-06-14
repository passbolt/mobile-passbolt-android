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

package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

class SecretParserTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testParserModule)
    }

    private val secretParser: SecretParser by inject()

    @Test
    fun `password should parse correct for password string secret`() {
        val secret = "\\\"!@#_$%^&*()".toByteArray()

        val extracted = secretParser.extractPassword(SIMPLE_PASSWORD, secret)

        assertThat(extracted).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        assertThat((extracted as DecryptedSecretOrError.DecryptedSecret).secret).isEqualTo(String(secret))
    }

    @Test
    fun `password and description should parse correct for password description secret`() {
        val secret = "{\"password\":\"\\\"!@#_\$%^&*()\", \"description\":\"desc\"}".toByteArray()

        val password = secretParser.extractPassword(PASSWORD_WITH_DESCRIPTION, secret)
        val description = secretParser.extractDescription(PASSWORD_WITH_DESCRIPTION, secret)

        assertThat(password).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        assertThat((password as DecryptedSecretOrError.DecryptedSecret).secret).isEqualTo("\"!@#_\$%^&*()")
        assertThat(description).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        assertThat((description as DecryptedSecretOrError.DecryptedSecret).secret).isEqualTo("desc")
    }

    @Test
    fun `totp should parse correct for standalone totp secret`() {
        val secret = ("{" +
                "\"totp\":{" +
                "\"digits\":6," +
                "\"period\":30," +
                "\"algorithm\":\"SHA256\"," +
                "\"secret_key\":\"secret\"" + "}" +
                "}").toByteArray()

        val totp = secretParser.extractTotpData(STANDALONE_TOTP, secret)

        assertThat(totp).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        val totpData = (totp as DecryptedSecretOrError.DecryptedSecret).secret
        assertThat(totpData.digits).isEqualTo(6)
        assertThat(totpData.period).isEqualTo(30)
        assertThat(totpData.algorithm).isEqualTo("SHA256")
        assertThat(totpData.key).isEqualTo("secret")
    }

    @Test
    fun `password description and totp should parse correct for password description totp secret`() {
        val secret = ("{" +
                "\"password\":\"pass\"," +
                "\"description\":\"desc\"," +
                "\"totp\":{" +
                "\"digits\":6," +
                "\"period\":30," +
                "\"algorithm\":\"SHA256\"," +
                "\"secret_key\":\"secret\"" +
                "}" +
                "}").toByteArray()

        val password = secretParser.extractPassword(PASSWORD_DESCRIPTION_TOTP, secret)
        val description = secretParser.extractDescription(PASSWORD_DESCRIPTION_TOTP, secret)
        val totp = secretParser.extractTotpData(PASSWORD_DESCRIPTION_TOTP, secret)

        assertThat(password).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        assertThat((password as DecryptedSecretOrError.DecryptedSecret).secret).isEqualTo("pass")
        assertThat(description).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        assertThat((description as DecryptedSecretOrError.DecryptedSecret).secret).isEqualTo("desc")
        assertThat(totp).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        val totpData = (totp as DecryptedSecretOrError.DecryptedSecret).secret
        assertThat(totpData.digits).isEqualTo(6)
        assertThat(totpData.period).isEqualTo(30)
        assertThat(totpData.algorithm).isEqualTo("SHA256")
        assertThat(totpData.key).isEqualTo("secret")
    }
}
