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

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.serializers.gson.ResourceListDeserializer
import com.passbolt.mobile.android.serializers.gson.ResourceListItemDeserializer
import com.passbolt.mobile.android.serializers.gson.ResourceTypesListDeserializer
import com.passbolt.mobile.android.serializers.gson.SingleResourceDeserializer
import com.passbolt.mobile.android.serializers.gson.serializer.ZonedDateTimeSerializer
import com.passbolt.mobile.android.serializers.gson.strictTypeAdapters
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.jsonschema.jsonSchemaModule
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.ZonedDateTime
import java.util.UUID

internal const val RESOURCE_DTO_GSON = "RESOURCE_DTO_GSON"

val serializersModule = module {
    jsonSchemaModule()

    singleOf(::JsonSchemaValidationRunner)
    singleOf(::ResourceListDeserializer)
    singleOf(::ResourceTypesListDeserializer)
    singleOf(::ZonedDateTimeSerializer)
    single {
        SingleResourceDeserializer(
            resourceTypeIdToSlugMappingProvider = get(),
            jsonSchemaValidationRunner = get(),
            gson = get(named(RESOURCE_DTO_GSON))
        )
    }
    single { (resourceTypeIdToSlugMapping: Map<UUID, String>, supportedResourceTypesIds: Set<UUID>) ->
        ResourceListItemDeserializer(
            jsonSchemaValidationRunner = get(),
            gson = get(named(RESOURCE_DTO_GSON)),
            resourceTypeIdToSlugMapping = resourceTypeIdToSlugMapping,
            supportedResourceTypesIds = supportedResourceTypesIds
        )
    }

    single {
        GsonBuilder()
            .serializeNulls()
            .apply {
                strictTypeAdapters.forEach {
                    registerTypeAdapter(it.key, it.value)
                }
                registerTypeAdapter(
                    object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
                    get<ResourceListDeserializer>()
                )
                registerTypeAdapter(
                    object : TypeToken<List<@JvmSuppressWildcards ResourceTypeDto>>() {}.type,
                    get<ResourceTypesListDeserializer>()
                )
                registerTypeAdapter(ZonedDateTime::class.java, get<ZonedDateTimeSerializer>())
                registerTypeAdapter(
                    ResourceResponseDto::class.java,
                    get<SingleResourceDeserializer>()
                )
            }
            .create()
    }

    single(named(RESOURCE_DTO_GSON)) {
        GsonBuilder()
            .apply {
                strictTypeAdapters.forEach {
                    registerTypeAdapter(it.key, it.value)
                }
            }
            .create()
    }
}
