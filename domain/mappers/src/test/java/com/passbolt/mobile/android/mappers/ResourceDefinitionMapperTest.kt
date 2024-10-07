package com.passbolt.mobile.android.mappers

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.entity.resource.ResourceField
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import java.io.File
import java.io.FileReader
import java.nio.file.Paths

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
class ResourceDefinitionMapperTest : KoinTest {

    private val gson: Gson by inject()
    private val mapper: ResourceTypesModelMapper by inject()

    private lateinit var resourceTypesResponse: List<ResourceTypeDto>

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testMappersModule)
    }

    @Before
    fun setUp() {
        val resourceDirectory = Paths.get("src", "test", "resources").toFile().absolutePath
        val resourceFile = File(resourceDirectory, "resource-types-v2.json")

        val parsedType = object : TypeToken<List<ResourceTypeDto>>() {}.type
        resourceTypesResponse = gson.fromJson(FileReader(resourceFile), parsedType)
    }

    // TODO(v5) Uncomment later after JSON schema is added; currently parsing definition is disabled

//    @Test
//    fun `parsed model should contain correct fields for simple password resource type`() {
//        val model = mapper.map(resourceTypesResponse)
//
//        with(model[0].resourceFields) {
//            assertThat(size).isEqualTo(5)
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "name",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "username",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "uri",
//                    isSecret = false,
//                    maxLength = 1024,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "description",
//                    isSecret = false,
//                    maxLength = 10000,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "secret",
//                    isSecret = true,
//                    maxLength = 4096,
//                    isRequired = true,
//                    "string"
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `parsed model should contain correct fields for password with description resource type`() {
//        val model = mapper.map(resourceTypesResponse)
//
//        with(model[1].resourceFields) {
//            assertThat(size).isEqualTo(5)
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "name",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "username",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "uri",
//                    isSecret = false,
//                    maxLength = 1024,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "description",
//                    isSecret = true,
//                    maxLength = 10000,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "password",
//                    isSecret = true,
//                    maxLength = 4096,
//                    isRequired = true,
//                    "string"
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `parsed model should contain correct fields for standalone otp resource type`() {
//        val model = mapper.map(resourceTypesResponse)
//
//        with(model[2].resourceFields) {
//            assertThat(size).isEqualTo(6)
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "name",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "uri",
//                    isSecret = false,
//                    maxLength = 1024,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "secret_key",
//                    isSecret = true,
//                    maxLength = 1024,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "algorithm",
//                    isSecret = true,
//                    maxLength = 4,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "period",
//                    isSecret = true,
//                    maxLength = null,
//                    isRequired = false,
//                    "number"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "digits",
//                    isSecret = true,
//                    maxLength = null,
//                    isRequired = true,
//                    "number"
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `parsed model should contain correct fields for standalone password with otp resource type`() {
//        val model = mapper.map(resourceTypesResponse)
//
//        with(model[3].resourceFields) {
//            assertThat(size).isEqualTo(9)
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "name",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "username",
//                    isSecret = false,
//                    maxLength = 255,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "uri",
//                    isSecret = false,
//                    maxLength = 1024,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "description",
//                    isSecret = true,
//                    maxLength = 10000,
//                    isRequired = false,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "password",
//                    isSecret = true,
//                    maxLength = 4096,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "secret_key",
//                    isSecret = true,
//                    maxLength = 1024,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "algorithm",
//                    isSecret = true,
//                    maxLength = 4,
//                    isRequired = true,
//                    "string"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "period",
//                    isSecret = true,
//                    maxLength = null,
//                    isRequired = false,
//                    "number"
//                )
//            )
//            assertThat(this).contains(
//                ResourceField(
//                    0,
//                    "digits",
//                    isSecret = true,
//                    maxLength = null,
//                    isRequired = true,
//                    "number"
//                )
//            )
//        }
//    }
}
