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

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.dto.PassphraseNotInCacheException
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.dto.response.ResourceResponseV4Dto
import com.passbolt.mobile.android.dto.response.ResourceResponseV5Dto
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.DECRYPT
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.allSlugs
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.lang.reflect.Type
import java.util.UUID

open class SingleResourceDeserializer(
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val getLocalMetadataKeys: GetLocalMetadataKeysUseCase,
    private val gson: Gson
) : JsonDeserializer<ResourceResponseDto?>, KoinComponent {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ResourceResponseDto? {
        if (json == null || context == null) {
            Timber.e("Json element or deserialization context was null: (${json == null}), (${context == null}")
            return null
        }

        // done on the parser thread
        return runBlocking {
            val resourceTypeIdToSlugMapping = resourceTypeIdToSlugMappingProvider
                .provideMappingForSelectedAccount()

            val supportedResourceTypesIds = resourceTypeIdToSlugMapping
                .filter { it.value in allSlugs }
                .keys

            val metadataKeys = getLocalMetadataKeys.execute(GetLocalMetadataKeysUseCase.Input(DECRYPT))
            val metadataDecryptor = get<MetadataDecryptor> { parametersOf(metadataKeys) }

            val resourceTypeId = json.asJsonObject[SerializedNames.RESOURCE_TYPE_ID].asString
            val slug = resourceTypeIdToSlugMapping[UUID.fromString(resourceTypeId)]

            if (!isSupported(resourceTypeId, supportedResourceTypesIds)) {
                Timber.d("Unsupported resource type id: $resourceTypeId, skipping")
                null
            } else if (json.isJsonObject && !json.asJsonObject.isJsonNull) {
                try {
                    if (slug in SupportedContentTypes.v4Slugs) {
                        val resource = gson.fromJson(json, ResourceResponseV4Dto::class.java)

                        if (isValid(resource.resourceTypeId, json.toString(), resourceTypeIdToSlugMapping)) {
                            resource
                        } else {
                            Timber.d("Invalid resource found id=(${resource.id}, skipping")
                            null
                        }
                    } else if (slug in SupportedContentTypes.v5Slugs) {
                        val resource = gson.fromJson(json, ResourceResponseV5Dto::class.java)

                        val decryptedMetadataResult = metadataDecryptor.decryptMetadata(resource)

                        if (decryptedMetadataResult is MetadataDecryptor.Output.Success && isValid(
                                resource.resourceTypeId,
                                decryptedMetadataResult.decryptedMetadata,
                                resourceTypeIdToSlugMapping
                            )
                        ) {
                            resource.copy(metadata = decryptedMetadataResult.decryptedMetadata)
                        } else {
                            Timber.d("Invalid resource found id=(${resource.id}, skipping")
                            null
                        }
                    } else {
                        @Suppress("UseRequire")
                        throw IllegalArgumentException("Unsupported resource type slug: $slug")
                    }
                } catch (e: PassphraseNotInCacheException) {
                    // re-throw this exception for to be mapped to Unauthenticated result
                    Timber.d("Passphrase not in cache; Re-throwing to show auth screen")
                    throw e
                } catch (e: Exception) {
                    Timber.e("Error when deserializing list item resource: ${e.message}")
                    null
                }
            } else {
                Timber.e("Encountered a null root json element when parsing resources")
                null
            }
        }
    }

    private suspend fun isValid(
        resourceTypeId: UUID,
        resourceJson: String,
        resourceTypeIdToSlugMapping: Map<UUID, String>
    ): Boolean {
        val resourceTypeSlug = resourceTypeIdToSlugMapping[resourceTypeId]
        return if (resourceTypeSlug != null) {
            jsonSchemaValidationRunner.isResourceValid(resourceJson, resourceTypeSlug)
        } else {
            return false
        }
    }

    private fun isSupported(resourceTypeId: String, supportedResourceTypesIds: Set<UUID>) =
        UUID.fromString(resourceTypeId) in supportedResourceTypesIds
}
