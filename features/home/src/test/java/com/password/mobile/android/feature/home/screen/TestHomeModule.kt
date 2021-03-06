package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.commonresource.usecase.RebuildResourceTablesUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.password.mobile.android.feature.home.TestCoroutineLaunchContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
internal val mockFoldersInteractor = mock<FoldersInteractor>()
internal val mockGetSubFoldersUseCase = mock<GetLocalSubFoldersForFolderUseCase>()
internal val mockGetSubFoldersResourcesUseCase = mock<GetLocalSubFolderResourcesFilteredUseCase>()
internal val mockGetLocalResourcesAndFoldersUseCase = mock<GetLocalResourcesAndFoldersUseCase>()

@ExperimentalCoroutinesApi
val testHomeModule = module {
    factory { resourcesInteractor }
    factory {
        ResourceModelMapper(
            initialsProvider = get()
        )
    }
    factory { getSelectedAccountDataUseCase }
    factory { fetchAndUpdateDatabaseUseCase }
    factory { InitialsProvider() }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory { SearchableMatcher() }
    factory { mockSecretParser }
    factory { mockResourceTypeFactory }
    factory { resourceMenuModelMapper }
    factory<HomeContract.Presenter> {
        HomePresenter(
            coroutineLaunchContext = get(),
            getSelectedAccountDataUseCase = get(),
            secretInteractor = mockSecretInteractor,
            searchableMatcher = get(),
            resourceTypeFactory = get(),
            secretParser = get(),
            resourceMenuModelMapper = get(),
            deleteResourceUseCase = mockDeleteResourceUseCase,
            getLocalResourcesUseCase = mockGetLocalResourcesUseCase,
            getLocalResourcesAndFoldersUseCase = mockGetLocalResourcesAndFoldersUseCase,
            getLocalSubFoldersForFolderUseCase = mockGetSubFoldersUseCase,
            getLocalResourcesFiltered = mockGetSubFoldersResourcesUseCase
        )
    }
}
