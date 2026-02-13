/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormValidator.Companion.URI_MAX_LENGTH
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AdditionalUrisFormValidatorTest {
    private lateinit var validator: AdditionalUrisFormValidator

    @Before
    fun setUp() {
        validator = AdditionalUrisFormValidator()
    }

    @Test
    fun `validateAll should return no errors when all uris are valid`() {
        val uuid = UUID.randomUUID()
        val input =
            AdditionalUrisValidationInput(
                mainUri = MAIN_URI,
                additionalUris =
                    linkedMapOf(
                        uuid to AdditionalUriItemState(uri = ADDITIONAL_URI),
                    ),
            )

        val result = validator.validateAll(input)

        assertThat(result.hasErrors).isFalse()
        assertThat(result.mainUriError).isNull()
        assertThat(result.additionalUris[uuid]?.error).isNull()
    }

    @Test
    fun `validateAll should return error for main uri when too long`() {
        val longUri = "a".repeat(URI_MAX_LENGTH + 1)
        val input =
            AdditionalUrisValidationInput(
                mainUri = longUri,
                additionalUris = linkedMapOf(),
            )

        val result = validator.validateAll(input)

        assertThat(result.hasErrors).isTrue()
        assertThat(result.mainUriError).isEqualTo(URI_MAX_LENGTH.toString())
    }

    @Test
    fun `validateAll should return error for additional uri when too long`() {
        val uuid = UUID.randomUUID()
        val longUri = "a".repeat(URI_MAX_LENGTH + 1)
        val input =
            AdditionalUrisValidationInput(
                mainUri = MAIN_URI,
                additionalUris =
                    linkedMapOf(
                        uuid to AdditionalUriItemState(uri = longUri),
                    ),
            )

        val result = validator.validateAll(input)

        assertThat(result.hasErrors).isTrue()
        assertThat(result.mainUriError).isNull()
        assertThat(result.additionalUris[uuid]?.error).isEqualTo(URI_MAX_LENGTH.toString())
    }

    @Test
    fun `validateAll should return errors for both main and additional uris when both too long`() {
        val uuid = UUID.randomUUID()
        val longUri = "a".repeat(URI_MAX_LENGTH + 1)
        val input =
            AdditionalUrisValidationInput(
                mainUri = longUri,
                additionalUris =
                    linkedMapOf(
                        uuid to AdditionalUriItemState(uri = longUri),
                    ),
            )

        val result = validator.validateAll(input)

        assertThat(result.hasErrors).isTrue()
        assertThat(result.mainUriError).isEqualTo(URI_MAX_LENGTH.toString())
        assertThat(result.additionalUris[uuid]?.error).isEqualTo(URI_MAX_LENGTH.toString())
    }

    @Test
    fun `validateAll should validate multiple additional uris independently`() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val validUri = "https://valid.com"
        val longUri = "a".repeat(URI_MAX_LENGTH + 1)
        val input =
            AdditionalUrisValidationInput(
                mainUri = MAIN_URI,
                additionalUris =
                    linkedMapOf(
                        uuid1 to AdditionalUriItemState(uri = validUri),
                        uuid2 to AdditionalUriItemState(uri = longUri),
                    ),
            )

        val result = validator.validateAll(input)

        assertThat(result.hasErrors).isTrue()
        assertThat(result.additionalUris[uuid1]?.error).isNull()
        assertThat(result.additionalUris[uuid2]?.error).isEqualTo(URI_MAX_LENGTH.toString())
    }

    @Test
    fun `validateAll should preserve uri values in result`() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val input =
            AdditionalUrisValidationInput(
                mainUri = MAIN_URI,
                additionalUris =
                    linkedMapOf(
                        uuid1 to AdditionalUriItemState(uri = ADDITIONAL_URI),
                        uuid2 to AdditionalUriItemState(uri = "https://second.com"),
                    ),
            )

        val result = validator.validateAll(input)

        assertThat(result.additionalUris[uuid1]?.uri).isEqualTo(ADDITIONAL_URI)
        assertThat(result.additionalUris[uuid2]?.uri).isEqualTo("https://second.com")
    }

    @Test
    fun `validateAll should return no errors for empty additional uris list`() {
        val input =
            AdditionalUrisValidationInput(
                mainUri = MAIN_URI,
                additionalUris = linkedMapOf(),
            )

        val result = validator.validateAll(input)

        assertThat(result.hasErrors).isFalse()
        assertThat(result.mainUriError).isNull()
        assertThat(result.additionalUris).isEmpty()
    }

    private companion object {
        const val MAIN_URI = "https://main.passbolt.com"
        const val ADDITIONAL_URI = "https://additional.passbolt.com"
    }
}
