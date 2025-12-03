package com.passbolt.mobile.android.permissions.permissions

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.metadata.usecase.CanShareResourceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
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

internal val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
internal val mockResourceShareInteractor = mock<ResourceShareInteractor>()
internal val mockHomeDataInteractor = mock<HomeDataInteractor>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockGetLocalFolderPermissionsUseCase = mock<GetLocalFolderPermissionsUseCase>()
internal val mockGetLocalFolderUseCase = mock<GetLocalFolderDetailsUseCase>()
internal val mockResourceTypeIdToSlugMappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()
internal val mockMetadataPrivateKeysHelperInteractor = mock<MetadataPrivateKeysHelperInteractor>()
internal val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
internal val mockCanShareResourceUseCase = mock<CanShareResourceUseCase>()

@ExperimentalCoroutinesApi
internal val testResourcePermissionsModule =
    module {
        factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
        factory { PermissionModelUiComparator() }
        factory<PermissionsContract.Presenter> {
            PermissionsPresenter(
                getLocalResourcePermissionsUseCase = mockGetLocalResourcePermissionsUseCase,
                getLocalResourceUseCase = mockGetLocalResourceUseCase,
                getLocalFolderPermissionsUseCase = mockGetLocalFolderPermissionsUseCase,
                permissionModelUiComparator = get(),
                getLocalFolderUseCase = mockGetLocalFolderUseCase,
                resourceShareInteractor = mockResourceShareInteractor,
                homeDataInteractor = mockHomeDataInteractor,
                coroutineLaunchContext = get(),
                resourceTypeIdToSlugMappingProvider = mockResourceTypeIdToSlugMappingProvider,
                metadataPrivateKeysHelperInteractor = mockMetadataPrivateKeysHelperInteractor,
                canShareResourceUseCase = mockCanShareResourceUseCase,
            )
        }
        single(named(JSON_MODEL_GSON)) { Gson() }
        single {
            Configuration
                .builder()
                .jsonProvider(GsonJsonProvider())
                .mappingProvider(GsonMappingProvider())
                .options(EnumSet.noneOf(Option::class.java))
                .build()
        }
        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
        single { mockResourceUpdateActionsInteractor }
        singleOf(::SessionRefreshTrackingFlow)
        singleOf(::DataRefreshTrackingFlow)
    }
