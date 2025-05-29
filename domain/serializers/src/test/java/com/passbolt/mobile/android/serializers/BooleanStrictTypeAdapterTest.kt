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

package com.passbolt.mobile.android.serializers

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.passbolt.mobile.android.serializers.gson.BooleanStrictTypeAdapter
import org.junit.Assert.assertThrows
import org.junit.Test

class BooleanStrictTypeAdapterTest {
    private data class TestModel(
        val testField: Boolean,
    )

    private val gson =
        GsonBuilder()
            .registerTypeAdapter(
                Boolean::class.java,
                BooleanStrictTypeAdapter(),
            ).create()

    @Test
    fun `parsing invalid types should throw exception`() {
        val invalidDataTypes =
            listOf(
                "{\"testField\": 1}",
                "{\"testField\": 1.0}",
                "{\"testField\": \"true\"}",
                "{\"testField\": {}}",
                "{\"testField\": []}",
            )

        invalidDataTypes.forEach {
            assertThrows(Exception::class.java) {
                gson.fromJson(it, TestModel::class.java)
            }
        }
    }

    @Test
    fun `parsing null should be allowed`() {
        val nullDataTypes =
            listOf(
                "{}",
                "{\"testField\": null}",
            )

        nullDataTypes.forEach {
            val parsed = gson.fromJson(it, TestModel::class.java)
            assertThat(parsed.testField).isFalse()
        }
    }

    @Test
    fun `parsing valid data type should work`() {
        val validParsedValues = listOf(true, false)
        val validDataTypes =
            listOf(
                "{\"testField\": true}",
                "{\"testField\": false}",
            )

        validDataTypes.forEachIndexed { index, value ->
            val parsed = gson.fromJson(value, TestModel::class.java)
            assertThat(parsed.testField).isEqualTo(validParsedValues[index])
        }
    }
}
