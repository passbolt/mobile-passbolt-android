package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonresource.FavouritesInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.database.impl.resourcetypes.ResourceTypesDao
import com.passbolt.mobile.android.feature.resources.actions.ResourceActionsInteractor
import com.passbolt.mobile.android.feature.resources.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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

internal val mockSecretInteractor = mock<SecretInteractor>()
internal val mockResourceTypesDao = mock<ResourceTypesDao>()
internal val mockResourceDatabase = mock<ResourceDatabase> {
    on { resourceTypesDao() }.doReturn(mockResourceTypesDao)
}
internal val mockDatabaseProvider = mock<DatabaseProvider> {
    on { get(any()) }.doReturn(mockResourceDatabase)
}
internal val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase> {
    on { execute(Unit) }.doReturn(GetSelectedAccountUseCase.Output("userId"))
}
internal val mockSecretParser = mock<SecretParser>()
internal val mockResourceTypeFactory = mock<ResourceTypeFactory>()
internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val resourceMenuModelMapper = ResourceMenuModelMapper()
internal val mockDeleteResourceUseCase = mock<DeleteResourceUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
internal val mockFavouritesInteractor = mock<FavouritesInteractor>()
internal val mockResourceTagsUseCase = mock<GetLocalResourceTagsUseCase>()

@ExperimentalCoroutinesApi
internal val testResourceDetailsModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<ResourceDetailsContract.Presenter> {
        ResourceDetailsPresenter(
            databaseProvider = mockDatabaseProvider,
            getSelectedAccountUseCase = mockGetSelectedAccountUseCase,
            getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
            resourceMenuModelMapper = resourceMenuModelMapper,
            getLocalResourceUseCase = mockGetLocalResourceUseCase,
            getLocalResourcePermissionsUseCase = mockGetLocalResourcePermissionsUseCase,
            coroutineLaunchContext = get(),
            getLocalResourceTagsUseCase = mockResourceTagsUseCase
        )
    }
    scope<ResourceDetailsPresenter> {
        factory { (resource: ResourceModel) ->
            ResourceActionsInteractor(resource)
        }
        factory { (
                      resource: ResourceModel,
                      needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
                      sessionRefreshedFlow: StateFlow<Unit?>
                  ) ->
            ResourceAuthenticatedActionsInteractor(
                needSessionRefreshFlow,
                sessionRefreshedFlow,
                resource,
                resourceTypeFactory = mockResourceTypeFactory,
                secretParser = mockSecretParser,
                secretInteractor = mockSecretInteractor,
                favouritesInteractor = mockFavouritesInteractor,
                deleteResourceUseCase = mockDeleteResourceUseCase
            )
        }
    }
}
