package com.password.mobile.android.feature.home.screen

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.autofill.urlmatcher.AutofillUrlMatcher
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetLocalGroupsWithShareItemsCountUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.RebuildResourceTablesUseCase
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesFilteredByTagUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.core.tags.usecase.db.GetLocalTagsUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
import java.util.EnumSet

internal val mockResourcesInteractor = mock<ResourceInteractor>()
internal val mockGetSelectedAccountDataUseCase = mock<GetSelectedAccountDataUseCase>()
internal val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val mockFetchAndUpdateDatabaseUseCase = mock<RebuildResourceTablesUseCase>()
internal val mockSecretParser = mock<SecretParser>()
internal val mockGetLocalResourcesUseCase = mock<GetLocalResourcesUseCase>()
internal val mockGetSubFoldersUseCase = mock<GetLocalSubFoldersForFolderUseCase>()
internal val mockGetSubFoldersResourcesUseCase = mock<GetLocalSubFolderResourcesFilteredUseCase>()
internal val mockGetLocalResourcesAndFoldersUseCase = mock<GetLocalResourcesAndFoldersUseCase>()
internal val mockGetLocalTagsUseCase = mock<GetLocalTagsUseCase>()
internal val mockGetLocalResourcesWithTagsUseCase = mock<GetLocalResourcesWithTagUseCase>()
internal val mockGetLocalGroupsWithItemCountUseCase = mock<GetLocalGroupsWithShareItemsCountUseCase>()
internal val mockGetLocalResourcesWithGroupsUseCase = mock<GetLocalResourcesWithGroupUseCase>()
internal val mockGetLocalResourcesFilteredByTagUseCase = mock<GetLocalResourcesFilteredByTagUseCase>()
internal val mockGetHomeDisplayPrefsUseCase = mock<GetHomeDisplayViewPrefsUseCase>()
internal val mockGetLocalFolderUseCase = mock<GetLocalFolderDetailsUseCase>()
internal val mockCreateResourceMoreMenuModelUseCase = mock<CreateResourceMoreMenuModelUseCase>()
internal val mockTotpParametersProvider = mock<TotpParametersProvider>()
internal val mockSecretPropertiesActionsInteractor = mock<SecretPropertiesActionsInteractor>()
internal val mockResourcePropertiesActionsInteractor = mock<ResourcePropertiesActionsInteractor>()
internal val mockResourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()
internal val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
internal val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()
internal val mockIdToSlugMappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()

@ExperimentalCoroutinesApi
val testHomeModule = module {
    factory { mockResourcesInteractor }
    factory { mockGetSelectedAccountDataUseCase }
    factory { mockFetchAndUpdateDatabaseUseCase }
    factory { mockSecretParser }
    factory { mockCreateResourceMoreMenuModelUseCase }
    single { mock<FullDataRefreshExecutor>() }
    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
    factoryOf(::InitialsProvider)
    factoryOf(::AutofillUrlMatcher)
    factoryOf(::HomeDisplayViewMapper)
    factoryOf(::SearchableMatcher)
    singleOf(::DeleteResourceIdlingResource)
    factory<HomeContract.Presenter> {
        HomePresenter(
            coroutineLaunchContext = get(),
            getSelectedAccountDataUseCase = get(),
            searchableMatcher = get(),
            getLocalResourcesUseCase = mockGetLocalResourcesUseCase,
            getLocalResourcesFilteredByTag = mockGetLocalResourcesFilteredByTagUseCase,
            getLocalSubFoldersForFolderUseCase = mockGetSubFoldersUseCase,
            getLocalResourcesAndFoldersUseCase = mockGetLocalResourcesAndFoldersUseCase,
            getLocalResourcesFiltered = mockGetSubFoldersResourcesUseCase,
            getLocalTagsUseCase = mockGetLocalTagsUseCase,
            getLocalResourcesWithTagUseCase = mockGetLocalResourcesWithTagsUseCase,
            getLocalGroupsWithShareItemsCountUseCase = mockGetLocalGroupsWithItemCountUseCase,
            getLocalResourcesWithGroupsUseCase = mockGetLocalResourcesWithGroupsUseCase,
            getHomeDisplayViewPrefsUseCase = mockGetHomeDisplayPrefsUseCase,
            homeModelMapper = get(),
            autofillMatcher = get(),
            getLocalFolderUseCase = mockGetLocalFolderUseCase,
            deleteResourceIdlingResource = get(),
            totpParametersProvider = mockTotpParametersProvider,
            getRbacRulesUseCase = mockGetRbacRulesUseCase,
            idToSlugMappingProvider = mockIdToSlugMappingProvider
        )
    }
    factory { mockResourceCommonActionsInteractor }
    factory { mockResourcePropertiesActionsInteractor }
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
