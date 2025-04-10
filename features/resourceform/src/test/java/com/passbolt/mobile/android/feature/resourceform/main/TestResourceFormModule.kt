package com.passbolt.mobile.android.feature.resourceform.main

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.feature.resourceform.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.usecase.GetEditContentTypeUseCase
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import com.passbolt.mobile.android.mappers.ResourceFormMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.Mockito.mock
import java.util.EnumSet

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

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()
internal val mockGetDefaultCreateContentTypeUseCase = mock<GetDefaultCreateContentTypeUseCase>()
internal val mockGetEditContentTypeUseCase = mock<GetEditContentTypeUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockFullDataRefreshExecutor = mock<FullDataRefreshExecutor>()

@OptIn(ExperimentalCoroutinesApi::class)
internal val testResourceFormModule = module {
    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
    factoryOf(::ResourceFormMapper)
    factoryOf(::EntropyViewMapper)
    singleOf(::ResourceModelHandler)
    factoryOf(::ResourceTypesUpdatesAdjacencyGraph)

    single { mockGetDefaultCreateContentTypeUseCase }
    single { mockGetEditContentTypeUseCase }
    single { mockGetLocalResourceUseCase }
    single { mockFullDataRefreshExecutor }
    single {
        mapOf(
            DefaultValue.NAME to "no name"
        )
    }

    factory<ResourceFormContract.Presenter> {
        ResourceFormPresenter(
            getPasswordPoliciesUseCase = mockGetPasswordPoliciesUseCase,
            secretGenerator = mockSecretGenerator,
            entropyCalculator = mockEntropyCalculator,
            getLocalResourceUseCase = get(),
            entropyViewMapper = get(),
            resourceFormMapper = get(),
            coroutineLaunchContext = get(),
            resourceModelHandler = get(),
            fullDataRefreshExecutor = get()
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
