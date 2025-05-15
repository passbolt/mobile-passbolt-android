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
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import kotlinx.coroutines.test.runTest
import net.jimblackler.jsonschemafriend.SchemaStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.util.UUID

class SecretParserTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testParserModule)
    }

    private val secretParser: SecretParser by inject()

    @Before
    fun setup() {
        mockJSFSchemaRepository.stub {
            on { schemaForSecret(ContentType.PasswordString.slug) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-string-secret-schema.json")
            )
            on { schemaForSecret(ContentType.PasswordAndDescription.slug) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-and-description-secret-schema.json")
            )
            on { schemaForSecret(ContentType.PasswordDescriptionTotp.slug) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-description-totp-secret-schema.json")
            )
            on { schemaForSecret(ContentType.Totp.slug) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/totp-secret-schema.json")
            )
        }
    }

    @Test
    fun `password should parse correct for password string secret`() = runTest {
        val secret = "\\\"!@#_$%^&*()"
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(resourceTypeId to ContentType.PasswordString.slug)
            )
        }

        val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

        assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        assertThat((secretResult as DecryptedSecretOrError.DecryptedSecret).secret.password).isEqualTo(secret)
    }

    @Test
    fun `password and description should parse correct for password description secret`() = runTest {
        val secret = "{\"password\":\"\\\"!@#_\$%^&*()\", \"description\":\"desc\"}"
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(resourceTypeId to ContentType.PasswordAndDescription.slug)
            )
        }

        val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

        assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        val parsedSecret = (secretResult as DecryptedSecretOrError.DecryptedSecret).secret
        assertThat(parsedSecret.secret).isEqualTo("\"!@#_\$%^&*()")
        assertThat(parsedSecret.description).isEqualTo("desc")
    }

    @Test
    fun `totp should parse correct for standalone totp secret`() = runTest {
        val secret = ("{" +
                "\"totp\":{" +
                "\"digits\":6," +
                "\"period\":30," +
                "\"algorithm\":\"SHA256\"," +
                "\"secret_key\":\"secret\"" + "}" +
                "}")
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(resourceTypeId to ContentType.Totp.slug)
            )
        }

        val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

        assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        val parsedSecret = (secretResult as DecryptedSecretOrError.DecryptedSecret).secret
        assertThat(parsedSecret.totp?.digits).isEqualTo(6)
        assertThat(parsedSecret.totp?.period).isEqualTo(30)
        assertThat(parsedSecret.totp?.algorithm).isEqualTo("SHA256")
        assertThat(parsedSecret.totp?.key).isEqualTo("secret")
    }

    @Test
    fun `password description and totp should parse correct for password description totp secret`() = runTest {
        val secret = ("{" +
                "\"password\":\"pass\"," +
                "\"description\":\"desc\"," +
                "\"totp\":{" +
                "\"digits\":6," +
                "\"period\":30," +
                "\"algorithm\":\"SHA256\"," +
                "\"secret_key\":\"secret\"" +
                "}" +
                "}")
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(resourceTypeId to ContentType.PasswordAndDescription.slug)
            )
        }

        val parsedSecretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

        assertThat(parsedSecretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
        val parsedSecret = (parsedSecretResult as DecryptedSecretOrError.DecryptedSecret).secret
        assertThat(parsedSecret.secret).isEqualTo("pass")
        assertThat(parsedSecret.description).isEqualTo("desc")
        assertThat(parsedSecret.totp?.digits).isEqualTo(6)
        assertThat(parsedSecret.totp?.period).isEqualTo(30)
        assertThat(parsedSecret.totp?.algorithm).isEqualTo("SHA256")
        assertThat(parsedSecret.totp?.key).isEqualTo("secret")
    }

    private companion object {
        private val resourceTypeId = UUID.randomUUID()
    }
}
