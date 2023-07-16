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

package com.passbolt.mobile.android.serializers.resourcetypeslistdeserializer

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.dto.response.ResourceDefinition
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.serializers.gson.ResourceTypesListDeserializer
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_STRING_SLUG
import org.junit.Test
import org.koin.test.KoinTest
import java.util.UUID

class ResourceTypesListDeserializerTest : KoinTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<List<@JvmSuppressWildcards ResourceTypeDto>>() {}.type,
            ResourceTypesListDeserializer()
        )
        .create()

    @Test
    fun `resources types with unsupported slugs should be filtered out`() {
        val resourceTypes = listOf(
            ResourceTypeDto(
                UUID.randomUUID(),
                "invalid_slug",
                "name",
                "description",
                ResourceDefinition(JsonObject(), JsonObject())
            ),
            ResourceTypeDto(
                UUID.randomUUID(),
                PASSWORD_STRING_SLUG,
                "name",
                "description",
                ResourceDefinition(JsonObject(), JsonObject())
            )
        )

        val listJson = gson.toJson(resourceTypes)
        val resultList = gson.fromJson<List<ResourceTypeDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceTypeDto>>() {}.type
        )

        assertThat(resultList.size).isEqualTo(1)
    }
}
