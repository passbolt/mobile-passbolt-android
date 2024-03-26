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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.mockJSFSchemaRepository
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.testParserModule
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_AND_DESCRIPTION_SLUG
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_DESCRIPTION_TOTP_SLUG
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_STRING_SLUG
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.TOTP_SLUG
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

class SecretValidationTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testParserModule)
    }

    private val secretValidationRunner: JsonSchemaValidationRunner by inject()
    private val gson: Gson by inject()

    @Before
    fun setup() {
        mockJSFSchemaRepository.stub {
            on { schemaForSecret(PASSWORD_STRING_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-string-secret-schema.json")
            )
            on { schemaForSecret(PASSWORD_AND_DESCRIPTION_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-and-description-secret-schema.json")
            )
            on { schemaForSecret(PASSWORD_DESCRIPTION_TOTP_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-description-totp-secret-schema.json")
            )
            on { schemaForSecret(TOTP_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/totp-secret-schema.json")
            )
        }
    }

    @Test
    fun `invalid secret for password string resource type should be rejected`() = runTest {
        val tooLongPassword = (0..PASSWORD_STRING_PASSWORD_MAX_LENGTH + 1)
            .joinToString { "a" }
        val tooLongPasswordJson = gson.toJson(tooLongPassword)

        val result = secretValidationRunner.isSecretValid(tooLongPasswordJson, PASSWORD_STRING_SLUG)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid secret for password and description resource type should be rejected`() = runTest {
        val tooLongPassword =
            (0..PASSWORD_AND_DESCRIPTION_PASSWORD_MAX_LENGTH + 1)
                .joinToString { "a" }
        val tooLongDescription =
            (0..PASSWORD_AND_DESCRIPTION_DESCRIPTION_MAX_LENGTH + 1)
                .joinToString { "a" }
        val invalidSecrets = listOf(
            JsonObject().apply {
                addProperty("description", "desc")
                addProperty("secret", tooLongPassword)
            },
            JsonObject().apply {
                addProperty("description", tooLongDescription)
                addProperty("secret", "pass")
            },
        ).map { gson.toJson(it) }

        val results = invalidSecrets
            .map { secretValidationRunner.isSecretValid(it, PASSWORD_AND_DESCRIPTION_SLUG) }

        assertThat(results.none { it }).isTrue()
    }

    @Test
    fun `invalid secret for totp resource type should be rejected`() = runTest {
        val invalidAlgorithm = "invalid_alg"
        val tooLongKey = (0..TOTP_KEY_MAX_LENGTH + 1)
            .joinToString { "a" }
        val tooFewDigits = TOTP_DIGITS_INCLUSIVE_MIN - 1
        val tooManyDigits = TOTP_DIGITS_INCLUSIVE_MAX + 1

        val invalidSecrets = listOf(
            JsonObject().apply {
                add("totp", JsonObject().apply {
                    addProperty("algorithm", invalidAlgorithm)
                    addProperty("secret_key", "A")
                    addProperty("digits", 6)
                    addProperty("perdiod", 1)
                })
            },
            JsonObject().apply {
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA1")
                    addProperty("secret_key", tooLongKey)
                    addProperty("digits", 6)
                    addProperty("perdiod", 1)
                })
            },
            JsonObject().apply {
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA1")
                    addProperty("secret_key", "A")
                    addProperty("digits", tooFewDigits)
                    addProperty("perdiod", 1)
                })
            },
            JsonObject().apply {
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA1")
                    addProperty("secret_key", "A")
                    addProperty("digits", tooManyDigits)
                    addProperty("perdiod", 1)
                })
            },
        ).map { gson.toJson(it) }

        val results = invalidSecrets
            .map { secretValidationRunner.isSecretValid(it, TOTP_SLUG) }

        assertThat(results.none { it }).isTrue()
    }

    @Test
    fun `invalid secret for password description totp resource type should be rejected`() = runTest {
        val tooLongPassword =
            (0..PASSWORD_DESCRIPTION_TOTP_PASSWORD_MAX_LENGTH + 1)
                .joinToString { "a" }
        val tooLongDescription =
            (0..PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MAX_LENGTH + 1)
                .joinToString { "a" }
        val invalidAlgorithm = "invalid_alg"
        val tooLongKey = (0..PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MAX_LENGTH + 1)
            .joinToString { "a" }
        val tooFewDigits =
            PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MIN - 1
        val tooManyDigits =
            PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MAX + 1
        val invalidSecrets = listOf(
            JsonObject().apply {
                addProperty("secret", tooLongPassword)
                addProperty("desc", "desc")
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA-256")
                    addProperty("secret_key", "A")
                    addProperty("digits", 6)
                    addProperty("period", 1)
                })
            },
            JsonObject().apply {
                addProperty("secret", "pass")
                addProperty("desc", tooLongDescription)
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA-256")
                    addProperty("secret_key", "A")
                    addProperty("digits", 6)
                    addProperty("period", 1)
                })
            },
            JsonObject().apply {
                addProperty("secret", "pass")
                addProperty("desc", "desc")
                add("totp", JsonObject().apply {
                    addProperty("algorithm", invalidAlgorithm)
                    addProperty("secret_key", "A")
                    addProperty("digits", 6)
                    addProperty("period", 1)
                })
            },
            JsonObject().apply {
                addProperty("secret", "pass")
                addProperty("desc", "desc")
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA1")
                    addProperty("secret_key", tooLongKey)
                    addProperty("digits", 6)
                    addProperty("period", 1)
                })
            },
            JsonObject().apply {
                addProperty("secret", "pass")
                addProperty("desc", "desc")
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA1")
                    addProperty("secret_key", "a")
                    addProperty("digits", tooFewDigits)
                    addProperty("period", 1)
                })
            },
            JsonObject().apply {
                addProperty("secret", "pass")
                addProperty("desc", "desc")
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA1")
                    addProperty("secret_key", "a")
                    addProperty("digits", tooManyDigits)
                    addProperty("period", 1)
                })
            },
        ).map { gson.toJson(it) }

        val results = invalidSecrets
            .map { secretValidationRunner.isSecretValid(it, PASSWORD_DESCRIPTION_TOTP_SLUG) }

        assertThat(results.none { it }).isTrue()
    }

    @Test
    fun `valid secret for password string resource type should not be rejected`() = runTest {
        val validSecret = gson.toJson("password")

        val result = secretValidationRunner.isSecretValid(validSecret, PASSWORD_STRING_SLUG)

        assertThat(result).isTrue()
    }

    @Test
    fun `valid secret for password and description resource type should not be rejected`() = runTest {
        val validSecrets = listOf(
            JsonObject().apply {
                addProperty("description", "desc")
                addProperty("password", "password")
            },
        ).map { gson.toJson(it) }

        val results = validSecrets
            .map { secretValidationRunner.isSecretValid(it, PASSWORD_AND_DESCRIPTION_SLUG) }

        assertThat(results.all { it }).isTrue()
    }

    @Test
    fun `valid secret for totp resource type should be not rejected`() = runTest {
        val validSecrets = listOf(
            JsonObject().apply {
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA-1")
                    addProperty("secret_key", "A")
                    addProperty("digits", 6)
                    addProperty("period", 1)
                })
            },
        ).map { gson.toJson(it) }

        val results = validSecrets
            .map { secretValidationRunner.isSecretValid(it, TOTP_SLUG) }

        assertThat(results.all { it }).isTrue()
    }

    @Test
    fun `valid secret for password description totp resource type should not be rejected`() = runTest {
        val validSecrets = listOf(
            JsonObject().apply {
                addProperty("description", "desc")
                addProperty("password", "password")
                add("totp", JsonObject().apply {
                    addProperty("algorithm", "SHA-1")
                    addProperty("secret_key", "A")
                    addProperty("digits", 6)
                    addProperty("period", 1)
                })
            }
        ).map { gson.toJson(it) }

        val results = validSecrets
            .map { secretValidationRunner.isSecretValid(it, PASSWORD_DESCRIPTION_TOTP_SLUG) }

        assertThat(results.all { it }).isTrue()
    }

    private companion object {
        const val PASSWORD_AND_DESCRIPTION_PASSWORD_MIN_LENGTH = 0
        const val PASSWORD_AND_DESCRIPTION_PASSWORD_MAX_LENGTH = 4096
        const val PASSWORD_AND_DESCRIPTION_DESCRIPTION_MIN_LENGTH = 0
        const val PASSWORD_AND_DESCRIPTION_DESCRIPTION_MAX_LENGTH = 10_000

        const val PASSWORD_DESCRIPTION_TOTP_PASSWORD_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_PASSWORD_MAX_LENGTH = 4096
        const val PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MAX_LENGTH = 10_000
        val PASSWORD_DESCRIPTION_TOTP_TOTP_ALGORITHM_ALLOWED_VALUES = hashSetOf("SHA1", "SHA256", "SHA512")
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MAX_LENGTH = 1024
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MIN = 6
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MAX = 8

        const val PASSWORD_STRING_PASSWORD_MIN_LENGTH = 0
        const val PASSWORD_STRING_PASSWORD_MAX_LENGTH = 4096

        val TOTP_ALGORITHM_ALLOWED_VALUES = hashSetOf("SHA1", "SHA256", "SHA512")
        const val TOTP_KEY_MIN_LENGTH = 0
        const val TOTP_KEY_MAX_LENGTH = 1024
        const val TOTP_DIGITS_INCLUSIVE_MIN = 6
        const val TOTP_DIGITS_INCLUSIVE_MAX = 8
    }
}
