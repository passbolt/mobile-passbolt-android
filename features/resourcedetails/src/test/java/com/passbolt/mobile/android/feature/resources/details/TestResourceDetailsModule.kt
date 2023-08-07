package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsContract
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsPresenter
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockResourceTypeFactory = mock<ResourceTypeFactory>()
internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
internal val mockFavouritesInteractor = mock<FavouritesInteractor>()
internal val mockResourceTagsUseCase = mock<GetLocalResourceTagsUseCase>()
internal val mockGetFolderLocationUseCase = mock<GetLocalFolderLocationUseCase>()
internal val mockGetResourceTypeWithFields = mock<GetResourceTypeWithFieldsByIdUseCase>()
internal val mockTotpParametersProvider = mock<TotpParametersProvider>()
internal val mockGetResourceTypeIdToSlugMappingUseCase = mock<GetResourceTypeIdToSlugMappingUseCase>()
internal val mockCreateResourceMoreMenuModelUseCase = mock<CreateResourceMoreMenuModelUseCase>()
internal val mockCreateOtpMoreMenuModelUseCase = mock<CreateOtpMoreMenuModelUseCase>()
internal val mockSecretPropertiesActionsInteractor = mock<SecretPropertiesActionsInteractor>()
internal val mockResourcePropertiesActionsInteractor = mock<ResourcePropertiesActionsInteractor>()
internal val mockResourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()
internal val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()

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
            getLocalResourceUseCase = mockGetLocalResourceUseCase,
            getLocalResourcePermissionsUseCase = mockGetLocalResourcePermissionsUseCase,
            getLocalResourceTagsUseCase = mockResourceTagsUseCase,
            getLocalFolderLocation = mockGetFolderLocationUseCase,
            getResourceTypeWithFieldsByIdUseCase = mockGetResourceTypeWithFields,
            totpParametersProvider = mockTotpParametersProvider,
            otpModelMapper = get(),
            getResourceTypeIdToSlugMappingUseCase = mockGetResourceTypeIdToSlugMappingUseCase,
            resourceTypeFactory = mockResourceTypeFactory,
            coroutineLaunchContext = get()
        )
    }
    factory { mockResourceCommonActionsInteractor }
    factory { mockResourcePropertiesActionsInteractor }
    factory { mockSecretPropertiesActionsInteractor }
    factory { mockResourceUpdateActionsInteractor }
}
