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
import com.passbolt.mobile.android.serializers.gson.IntStrictTypeAdapter
import org.junit.Assert.assertThrows
import org.junit.Test


class IntStrictTypeAdapterTest {

    private data class TestModel(val testField: Int)

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            Int::class.java,
            IntStrictTypeAdapter()
        )
        .create()

    @Test
    fun `parsing invalid types should throw exception`() {
        val invalidDataTypes = listOf(
            "{\"testField\": \"1.0\"}",
            "{\"testField\": ${Long.MAX_VALUE}}",
            "{\"testField\": true}",
            "{\"testField\": {}}",
            "{\"testField\": []}"
        )

        invalidDataTypes.forEach {
            assertThrows(Exception::class.java) {
                gson.fromJson(it, TestModel::class.java)
            }
        }
    }

    @Test
    fun `parsing null should be allowed`() {
        val nullDataTypes = listOf(
            "{}",
            "{\"testField\": null}",
        )

        nullDataTypes.forEach {
            val parsed = gson.fromJson(it, TestModel::class.java)
            assertThat(parsed.testField).isEqualTo(0)
        }
    }

    @Test
    fun `parsing valid data type should work`() {
        val validParsedValues = listOf(1, Int.MAX_VALUE)
        val validDataTypes = listOf(
            "{\"testField\": ${validParsedValues[0]}}",
            "{\"testField\": ${validParsedValues[1]}}"
        )

        validDataTypes.forEachIndexed { index, value ->
            val parsed = gson.fromJson(value, TestModel::class.java)
            assertThat(parsed.testField).isEqualTo(validParsedValues[index])
        }
    }
}
