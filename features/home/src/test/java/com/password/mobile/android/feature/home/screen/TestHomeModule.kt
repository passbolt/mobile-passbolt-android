package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.common.DomainProvider
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetLocalGroupsWithShareItemsCountUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.ResourceActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateToLinkedTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.RebuildResourceTablesUseCase
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesFilteredByTagUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.core.tags.usecase.db.GetLocalTagsUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
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
internal val mockCreateResourceMoreMenuModelUseCase = mock<CreateResourceMoreMenuModelUseCase>()
internal val mockUpdateLocalResourceUseCase = mock<UpdateLocalResourceUseCase>()
internal val mockUpdateToLinkedTotpResourceInteractor = mock<UpdateToLinkedTotpResourceInteractor>()

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
    factory { mockCreateResourceMoreMenuModelUseCase }
    factory { HomeDisplayViewMapper() }
    factory { DomainProvider() }
    single { mock<FullDataRefreshExecutor>() }
    single { DeleteResourceIdlingResource() }
    factory<HomeContract.Presenter> {
        HomePresenter(
            coroutineLaunchContext = get(),
            getSelectedAccountDataUseCase = get(),
            searchableMatcher = get(),
            createResourceMenuModelUseCase = get(),
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
            getLocalFolderUseCase = mockGetLocalFolderUseCase,
            deleteResourceIdlingResource = get(),
            updateToLinkedTotpResourceInteractor = mockUpdateToLinkedTotpResourceInteractor,
            secretInteractor = mockSecretInteractor,
            updateLocalResourceUseCase = mockUpdateLocalResourceUseCase
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
