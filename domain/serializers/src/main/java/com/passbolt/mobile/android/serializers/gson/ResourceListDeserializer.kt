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

package com.passbolt.mobile.android.serializers.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.serializers.SupportedContentTypes
import com.passbolt.mobile.android.serializers.gson.validation.ResourceValidationRunner
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.lang.reflect.Type
import java.util.UUID

open class ResourceListDeserializer(
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val resourceValidationRunner: ResourceValidationRunner
) : JsonDeserializer<List<ResourceResponseDto>>, KoinComponent {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<ResourceResponseDto> {
        if (json == null || context == null) {
            Timber.e("Json element or deserialization context was null: (${json == null}), (${context == null}")
            return emptyList()
        }

        // done on the parser thread
        val resourceTypeIdToSlugMapping = runBlocking {
            resourceTypeIdToSlugMappingProvider.provideMappingForSelectedAccount()
        }

        val supportedResourceTypesIds = resourceTypeIdToSlugMapping
            .filter { it.value in SupportedContentTypes.homeSlugs + SupportedContentTypes.totpSlugs }
            .keys

        return if (json.isJsonArray) {
            json.asJsonArray.mapNotNullTo(mutableListOf()) { jsonElement ->
                if (!jsonElement.isJsonNull) {
                    val resource = context.deserialize<ResourceResponseDto>(
                        jsonElement, ResourceResponseDto::class.java
                    )

                    if (isSupported(resource, supportedResourceTypesIds) &&
                        isValid(resource, resourceTypeIdToSlugMapping)
                    ) {
                        resource
                    } else {
                        Timber.d("Invalid resource found id=(${resource.id}, skipping")
                        null
                    }
                } else {
                    Timber.e("Encountered a null root json element when parsing resources")
                    null
                }
            }
        } else {
            emptyList()
        }
    }

    private fun isValid(resource: ResourceResponseDto, resourceTypeIdToSlugMapping: Map<UUID, String>): Boolean {
        val resourceTypeSlug = resourceTypeIdToSlugMapping[resource.resourceTypeId]
        return if (resourceTypeSlug != null) {
            resourceValidationRunner.isValid(resource, resourceTypeSlug)
        } else {
            return false
        }
    }

    private fun isSupported(resource: ResourceResponseDto, supportedResourceTypesIds: Set<UUID>) =
        resource.resourceTypeId in supportedResourceTypesIds
}
