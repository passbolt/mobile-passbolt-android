package com.passbolt.mobile.android.feature.resources.details

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsContract
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsPresenter
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
import java.util.EnumSet

internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
internal val mockFavouritesInteractor = mock<FavouritesInteractor>()
internal val mockResourceTagsUseCase = mock<GetLocalResourceTagsUseCase>()
internal val mockGetFolderLocationUseCase = mock<GetLocalFolderLocationUseCase>()
internal val mockTotpParametersProvider = mock<TotpParametersProvider>()
internal val mockSecretPropertiesActionsInteractor = mock<SecretPropertiesActionsInteractor>()
internal val mockResourcePropertiesActionsInteractor = mock<ResourcePropertiesActionsInteractor>()
internal val mockResourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()
internal val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
internal val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()
internal val mockResourceTypeIdToSlugMappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()

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
            totpParametersProvider = mockTotpParametersProvider,
            otpModelMapper = get(),
            coroutineLaunchContext = get(),
            getRbacRulesUseCase = mockGetRbacRulesUseCase,
            resourceDetailActionIdlingResource = mock(),
            idToSlugMappingProvider = mockResourceTypeIdToSlugMappingProvider
        )
    }
    factory { (resource: ResourceModel) ->
        ResourcePropertiesActionsInteractor(
            resource,
            idToSlugMappingProvider = mockResourceTypeIdToSlugMappingProvider
        )
    }
    factory { mockResourceCommonActionsInteractor }
    factory { mockSecretPropertiesActionsInteractor }
    factory { mockResourceUpdateActionsInteractor }
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
