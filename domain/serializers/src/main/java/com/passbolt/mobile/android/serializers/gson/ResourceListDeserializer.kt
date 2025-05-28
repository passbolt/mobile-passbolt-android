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
import com.passbolt.mobile.android.database.snapshot.ResourcesSnapshot
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.DECRYPT
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.allSlugs
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.lang.reflect.Type

open class ResourceListDeserializer(
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getLocalMetadataKeysUseCase: GetLocalMetadataKeysUseCase
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
        return runBlocking {
            val resourceTypeIdToSlugMapping = resourceTypeIdToSlugMappingProvider
                .provideMappingForSelectedAccount()

            val supportedResourceTypesIds = resourceTypeIdToSlugMapping
                .filter { it.value in allSlugs }
                .keys

            val metadataKeys = getLocalMetadataKeysUseCase.execute(GetLocalMetadataKeysUseCase.Input(DECRYPT))

            val resourcesSnapshot = get<ResourcesSnapshot>()

            val singleResourceDeserializer = get<ResourceListItemDeserializer> {
                parametersOf(resourceTypeIdToSlugMapping, supportedResourceTypesIds, metadataKeys, resourcesSnapshot)
            }

            if (json.isJsonArray) {
                json.asJsonArray.mapNotNullTo(mutableListOf()) { jsonElement ->
                    if (!jsonElement.isJsonNull) {
                        singleResourceDeserializer.deserialize(
                            jsonElement, ResourceResponseDto::class.java, context
                        )
                    } else {
                        Timber.e("Encountered a null root json element when parsing resources")
                        null
                    }
                }
            } else {
                emptyList()
            }
        }
    }
}
