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

package com.passbolt.mobile.android.serializers.resourcelistdeserializer.caching

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetLocalResourceTypesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.database.snapshot.ResourcesSnapshot
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.serializers.STRICT_ADAPTERS_ONLY_GSON
import com.passbolt.mobile.android.serializers.gson.MetadataDecryptor
import com.passbolt.mobile.android.serializers.gson.ResourceListDeserializer
import com.passbolt.mobile.android.serializers.gson.ResourceListItemDeserializer
import com.passbolt.mobile.android.serializers.gson.strictTypeAdapters
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.jimblackler.jsonschemafriend.Validator
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
import java.util.UUID

internal val mockIdToSlugMappingUseCase = mock<GetResourceTypeIdToSlugMappingUseCase>()
internal val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val mockMetadataDecryptor = mock<MetadataDecryptor>()
internal val mockGetLocalMetadataKeysUseCase = mock<GetLocalMetadataKeysUseCase>()
internal val mockResourcesSnapShot = mock<ResourcesSnapshot>()
internal val mockJsonSchemaValidationRunner = mock<JsonSchemaValidationRunner>()
internal val mockGetLocalResourceTypesUseCase = mock<GetLocalResourceTypesUseCase>()

@OptIn(ExperimentalCoroutinesApi::class)
val resourceListItemDeserializationTestModule = module {
    singleOf(::JsonSchemaValidationRunner)
    singleOf(::ResourceListDeserializer)
    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
    single { mock<OpenPgp>() }
    single { (resourceTypeIdToSlugMapping: Map<UUID, String>, supportedResourceTypesIds: Set<UUID>) ->
        ResourceListItemDeserializer(
            jsonSchemaValidationRunner = mockJsonSchemaValidationRunner,
            gson = get(named(STRICT_ADAPTERS_ONLY_GSON)),
            resourceTypeIdToSlugMapping = resourceTypeIdToSlugMapping,
            supportedResourceTypesIds = supportedResourceTypesIds,
            metadataDecryptor = mockMetadataDecryptor,
            resourcesSnapshot = get(),
            coroutineLaunchContext = get()
        )
    }
    singleOf(::ResourceTypeIdToSlugMappingProvider)
    single { Validator() }
    single { mockResourcesSnapShot }
    single { mockIdToSlugMappingUseCase }
    factory { mockGetSelectedAccountUseCase }
    single { mockGetLocalResourceTypesUseCase }
    single {
        GsonBuilder()
            .registerTypeAdapter(
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
                get<ResourceListDeserializer>()
            )
            .create()
    }
    single(named(STRICT_ADAPTERS_ONLY_GSON)) {
        GsonBuilder()
            .apply {
                strictTypeAdapters.forEach {
                    registerTypeAdapter(it.key, it.value)
                }
            }
            .create()
    }
    single { mockGetLocalMetadataKeysUseCase }
}
