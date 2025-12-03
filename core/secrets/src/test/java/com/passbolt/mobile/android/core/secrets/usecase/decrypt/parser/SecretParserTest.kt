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
import com.passbolt.mobile.android.jsonmodel.delegates.SecretCustomFieldType.BOOLEAN
import com.passbolt.mobile.android.jsonmodel.delegates.SecretCustomFieldType.NUMBER
import com.passbolt.mobile.android.jsonmodel.delegates.SecretCustomFieldType.PASSWORD
import com.passbolt.mobile.android.jsonmodel.delegates.SecretCustomFieldType.TEXT
import com.passbolt.mobile.android.jsonmodel.delegates.SecretCustomFieldType.URI
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5CustomFields
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Note
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
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testParserModule)
        }

    private val secretParser: SecretParser by inject()

    @Before
    fun setup() {
        mockJSFSchemaRepository.stub {
            on { schemaForSecret(PasswordString.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/password-string-secret-schema.json"),
                )
            on { schemaForSecret(PasswordAndDescription.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/password-and-description-secret-schema.json"),
                )
            on { schemaForSecret(PasswordDescriptionTotp.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/password-description-totp-secret-schema.json"),
                )
            on { schemaForSecret(Totp.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/totp-secret-schema.json"),
                )
            on { schemaForSecret(V5CustomFields.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/v5-custom-fields-secret-schema.json"),
                )
            on { schemaForSecret(V5Note.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/v5-note-secret-schema.json"),
                )
        }
    }

    @Test
    fun `password should parse correct for password string secret`() =
        runTest {
            val secret = "\\\"!@#_$%^&*()"
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(resourceTypeId to PasswordString.slug),
                )
            }

            val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

            assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
            assertThat((secretResult as DecryptedSecretOrError.DecryptedSecret).secret.password).isEqualTo(secret)
        }

    @Test
    fun `password and description should parse correct for password description secret`() =
        runTest {
            val secret =
                """
                {
                    "password": "\"!@#_$%^&*()",
                    "description": "desc"
                }
                """.trimIndent()
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(resourceTypeId to PasswordAndDescription.slug),
                )
            }

            val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

            assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
            val parsedSecret = (secretResult as DecryptedSecretOrError.DecryptedSecret).secret
            assertThat(parsedSecret.secret).isEqualTo("\"!@#_\$%^&*()")
            assertThat(parsedSecret.description).isEqualTo("desc")
        }

    @Test
    fun `totp should parse correct for standalone totp secret`() =
        runTest {
            val secret =
                """
                {
                    "totp": {
                        "digits": 6,
                        "period": 30,
                        "algorithm": "SHA256",
                        "secret_key": "secret"
                    }
                }
                """.trimIndent()
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(resourceTypeId to Totp.slug),
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
    fun `password description and totp should parse correct for password description totp secret`() =
        runTest {
            val secret =
                """
                {
                    "password": "pass",
                    "description": "desc",
                    "totp": {
                        "digits": 6,
                        "period": 30,
                        "algorithm": "SHA256",
                        "secret_key": "secret"
                    }
                }
                """.trimIndent()
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(resourceTypeId to PasswordAndDescription.slug),
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

    @Test
    fun `custom fields should parse correct for v5 custom fields secret`() =
        runTest {
            val secret =
                """
                {
                    "object_type": "PASSBOLT_SECRET_DATA",
                    "custom_fields": [
                        {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "type": "text",
                            "secret_value": "value1"
                        },
                        {
                            "id": "550e8400-e29b-41d4-a716-446655440001",
                            "type": "password",
                            "secret_value": "value2"
                        },
                        {
                            "id": "550e8400-e29b-41d4-a716-446655440002",
                            "type": "boolean",
                            "secret_value": true
                        },
                        {
                            "id": "550e8400-e29b-41d4-a716-446655440003",
                            "type": "number",
                            "secret_value": 8080
                        },
                        {
                            "id": "550e8400-e29b-41d4-a716-446655440004",
                            "type": "uri",
                            "secret_value": "https://passbolt.com"
                        }
                    ]
                }
                """.trimIndent()
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(resourceTypeId to V5CustomFields.slug),
                )
            }

            val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)
            assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
            val parsedSecret = (secretResult as DecryptedSecretOrError.DecryptedSecret).secret

            assertThat(parsedSecret.customFields).hasSize(5)

            val customFields = parsedSecret.customFields!!
            assertThat(customFields[0].type).isEqualTo(TEXT)
            assertThat(customFields[0].secretValue?.asString).isEqualTo("value1")

            assertThat(customFields[1].type).isEqualTo(PASSWORD)
            assertThat(customFields[1].secretValue?.asString).isEqualTo("value2")

            assertThat(customFields[2].type).isEqualTo(BOOLEAN)
            assertThat(customFields[2].secretValue?.asBoolean).isEqualTo(true)

            assertThat(customFields[3].type).isEqualTo(NUMBER)
            assertThat(customFields[3].secretValue?.asInt).isEqualTo(8080)

            assertThat(customFields[4].type).isEqualTo(URI)
            assertThat(customFields[4].secretValue?.asString).isEqualTo("https://passbolt.com")
        }

    @Test
    fun `note should parse correct for description secret`() =
        runTest {
            val secret =
                """
                {
                    "object_type": "PASSBOLT_SECRET_DATA",
                    "description": "desc"
                }
                """.trimIndent()
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(resourceTypeId to V5Note.slug),
                )
            }

            val secretResult = secretParser.parseSecret(resourceTypeId.toString(), secret)

            assertThat(secretResult).isInstanceOf(DecryptedSecretOrError.DecryptedSecret::class.java)
            val parsedSecret = (secretResult as DecryptedSecretOrError.DecryptedSecret).secret
            assertThat(parsedSecret.description).isEqualTo("desc")
        }

    private companion object {
        private val resourceTypeId = UUID.randomUUID()
    }
}
