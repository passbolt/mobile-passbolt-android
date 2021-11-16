package com.passbolt.mobile.android.mappers

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
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
        val resourceFile = File(resourceDirectory, "resource-types.json")

        val parsedType = object : TypeToken<List<ResourceTypeDto>>() {}.type
        resourceTypesResponse = gson.fromJson(FileReader(resourceFile), parsedType)
    }

    @Test
    fun `parsed model should contain correct fields count`() {
        val model = mapper.map(resourceTypesResponse)

        assertThat(model[0].resourceFields.size).isEqualTo(5)
        assertThat(model[1].resourceFields.size).isEqualTo(5)
    }

    @Test
    fun `parsed model should contain correct secret and raw fields count`() {
        val model = mapper.map(resourceTypesResponse)

        assertThat(model[0].resourceFields.filter { it.isSecret }.size).isEqualTo(1)
        assertThat(model[1].resourceFields.filter { it.isSecret }.size).isEqualTo(2)
    }

    @Test
    fun `parsed model should contain correct secret and raw fields`() {
        val model = mapper.map(resourceTypesResponse)

        val secretFieldNamesForAllResourceTypes = model
            .flatMap { it.resourceFields }
            .filter { it.isSecret }
            .map { it.name }

        assertThat(secretFieldNamesForAllResourceTypes.size).isEqualTo(3)
        assertThat(secretFieldNamesForAllResourceTypes).containsExactly("password", "description", "secret")
    }

    @Test
    fun `parsed model should contain correct required flag`() {
        val model = mapper.map(resourceTypesResponse)

        val requiredFieldNamesForAllResourceTypes = model
            .flatMap { it.resourceFields }
            .filter { it.isRequired }
            .map { it.name }

        assertThat(requiredFieldNamesForAllResourceTypes.size).isEqualTo(4)
        assertThat(requiredFieldNamesForAllResourceTypes).containsExactly("name", "secret", "name", "password")
    }
}
