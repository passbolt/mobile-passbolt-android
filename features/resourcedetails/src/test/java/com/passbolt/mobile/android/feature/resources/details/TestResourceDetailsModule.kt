package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.actions.ResourceActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsContract
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsPresenter
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
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
internal val mockSecretParser = mock<SecretParser>()
internal val mockResourceTypeFactory = mock<ResourceTypeFactory>()
internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val resourceMenuModelMapper = ResourceMenuModelMapper()
internal val mockDeleteResourceUseCase = mock<DeleteResourceUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
internal val mockFavouritesInteractor = mock<FavouritesInteractor>()
internal val mockResourceTagsUseCase = mock<GetLocalResourceTagsUseCase>()
internal val mockGetFolderLocationUseCase = mock<GetLocalFolderLocationUseCase>()
internal val mockGetResourceTypeWithFields = mock<GetResourceTypeWithFieldsByIdUseCase>()
internal val mockTotpParametersProvider = mock<TotpParametersProvider>()
internal val mockGetResourceTypeIdToSlugMappingUseCase = mock<GetResourceTypeIdToSlugMappingUseCase>()

@ExperimentalCoroutinesApi
internal val testResourceDetailsModule = module {
    single { mock<FullDataRefreshExecutor>() }
    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
    factoryOf(::OtpModelMapper)
    factoryOf(::InitialsProvider)
    factoryOf(::PermissionsModelMapper)
    factoryOf(::GroupsModelMapper)
    factoryOf(::UsersModelMapper)
    factory<ResourceDetailsContract.Presenter> {
        ResourceDetailsPresenter(
            getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
            resourceMenuModelMapper = resourceMenuModelMapper,
            getLocalResourceUseCase = mockGetLocalResourceUseCase,
            getLocalResourcePermissionsUseCase = mockGetLocalResourcePermissionsUseCase,
            getLocalResourceTagsUseCase = mockResourceTagsUseCase,
            coroutineLaunchContext = get(),
            getLocalFolderLocation = mockGetFolderLocationUseCase,
            getResourceTypeWithFieldsByIdUseCase = mockGetResourceTypeWithFields,
            totpParametersProvider = mockTotpParametersProvider,
            getResourceTypeIdToSlugMappingUseCase = mockGetResourceTypeIdToSlugMappingUseCase,
            otpModelMapper = get()
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
