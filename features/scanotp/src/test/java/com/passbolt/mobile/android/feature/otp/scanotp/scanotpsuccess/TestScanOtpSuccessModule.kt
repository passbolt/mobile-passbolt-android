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

package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
import java.util.EnumSet

internal val mockIdToSlugMappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()
internal val mockResourceCreateActionsInteractor = mock<ResourceCreateActionsInteractor>()
internal val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
internal val mockGetDefaultCreateContentTypeUseCase = mock<GetDefaultCreateContentTypeUseCase>()
internal val mockMetadataPrivateKeysHelperInteractor = mock<MetadataPrivateKeysHelperInteractor>()

@ExperimentalCoroutinesApi
internal val testScanOtpSuccessModule = module {
    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
    single { mockResourceCreateActionsInteractor }
    single { mockResourceUpdateActionsInteractor }

    factory<ScanOtpSuccessContract.Presenter> {
        ScanOtpSuccessPresenter(
            idToSlugMappingProvider = mockIdToSlugMappingProvider,
            getDefaultCreateContentTypeUseCase = mockGetDefaultCreateContentTypeUseCase,
            metadataPrivateKeysHelperInteractor = mockMetadataPrivateKeysHelperInteractor,
            coroutineLaunchContext = get()
        )
    }
    single(named(JSON_MODEL_GSON)) { Gson() }
    single {
        Configuration.builder()
            .jsonProvider(GsonJsonProvider())
            .mappingProvider(GsonMappingProvider())
            .options(EnumSet.noneOf(Option::class.java))
            .build()
    }
    singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
}
