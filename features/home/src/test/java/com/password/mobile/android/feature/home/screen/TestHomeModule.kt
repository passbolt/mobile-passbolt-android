package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.common.DomainProvider
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.progress.ProgressStackSynchronizer
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.RebuildResourceTablesUseCase
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.database.impl.folders.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.database.impl.folders.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.database.impl.folders.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.database.impl.folders.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.database.impl.groups.GetLocalGroupsWithShareItemsCountUseCase
import com.passbolt.mobile.android.database.impl.resourceandgroupscrossref.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.database.impl.resourceandtagcrossref.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcesFilteredByTagUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcesUseCase
import com.passbolt.mobile.android.database.impl.tags.GetLocalTagsUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.resourcedetails.actions.ResourceActionsInteractor
import com.passbolt.mobile.android.feature.resourcedetails.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val resourcesInteractor = mock<ResourceInteractor>()
internal val getSelectedAccountDataUseCase = mock<GetSelectedAccountDataUseCase>()
internal val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val fetchAndUpdateDatabaseUseCase = mock<RebuildResourceTablesUseCase>()
internal val mockSecretInteractor = mock<SecretInteractor>()
internal val mockSecretParser = mock<SecretParser>()
internal val mockResourceTypeFactory = mock<ResourceTypeFactory>()
internal val resourceMenuModelMapper = ResourceMenuModelMapper()
internal val mockDeleteResourceUseCase = mock<DeleteResourceUseCase>()
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
internal val mockFavouritesInteractor = mock<FavouritesInteractor>()
internal val mockGetLocalFolderUseCase = mock<GetLocalFolderDetailsUseCase>()

@ExperimentalCoroutinesApi
val testHomeModule = module {
    factory { resourcesInteractor }
    factory { getSelectedAccountDataUseCase }
    factory { fetchAndUpdateDatabaseUseCase }
    factory { InitialsProvider() }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory { SearchableMatcher() }
    factory { mockSecretParser }
    factory { mockResourceTypeFactory }
    factory { resourceMenuModelMapper }
    factory { HomeDisplayViewMapper() }
    factory { DomainProvider() }
    factory { ProgressStackSynchronizer() }
    factory<HomeContract.Presenter> {
        HomePresenter(
            coroutineLaunchContext = get(),
            getSelectedAccountDataUseCase = get(),
            searchableMatcher = get(),
            resourceMenuModelMapper = get(),
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
            domainProvider = get(),
            getLocalFolderUseCase = mockGetLocalFolderUseCase
        )
    }
    scope<HomePresenter> {
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
