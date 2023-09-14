package com.password.mobile.android.feature.home.screen

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesFilteredByTagUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.entity.home.HomeDisplayView
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayViewModel
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.DefaultFilterModel
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@ExperimentalCoroutinesApi
class HomePresenterTest : KoinTest {

    private val presenter: HomeContract.Presenter by inject()
    private val view: HomeContract.View = mock()
    private val mockFullDataRefreshExecutor: FullDataRefreshExecutor by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testHomeModule)
    }

    @Before
    fun setUp() {
        whenever(getSelectedAccountUseCase.execute(anyOrNull())).thenReturn(
            GetSelectedAccountUseCase.Output("id")
        )
        mockGetLocalResourcesAndFoldersUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesAndFoldersUseCase.Output.Success(
                emptyList(),
                emptyList()
            )
        }
        mockGetLocalResourcesFilteredByTagUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesFilteredByTagUseCase.Output(
                emptyList()
            )
        }
    }

    @Test
    fun `user avatar should be displayed when provided`() = runTest {
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        val url = "avatar_url"

        mockAccountData(url)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view).displaySearchAvatar(eq(url))
    }

    @Test
    fun `search input end icon should switch correctly based on input`() {
        val url = "avatar_url"
        mockAccountData(url)
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        presenter.searchTextChange("abc")
        presenter.searchTextChange("")

        verify(view, times(2)).displaySearchAvatar(url)
        verify(view).displaySearchClearIcon()
    }

    @Test
    fun `all fetched resources should be displayed when empty search text`() = runTest {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )

        mockAccountData(null)
        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view).showHomeScreenTitle(HomeDisplayViewModel.AllItems)
        verify(view).showAllItemsSearchHint()
        verify(view).hideBackArrow()
        verify(view).hideAddButton()
        verify(view).showAddButton()
        verify(view, times(2)).showItems(any(), any(), any(), any(), any(), any(), any(), any())
        verify(view).displaySearchAvatar(null)
        verify(view).hideRefreshProgress()
        verify(view).hideFolderMoreMenuIcon()
        verify(view).initSpeedDialFab(HomeDisplayViewModel.AllItems)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `refresh swiped should reload data with filter applied when search text entered`() = runTest {
        mockResourcesList()
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )
        mockAccountData(null)
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        val refreshFlow = MutableStateFlow(
            DataRefreshStatus.Finished(
                HomeDataInteractor.Output.Success
            )
        )
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(refreshFlow)
        whenever(mockFullDataRefreshExecutor.performFullDataRefresh()).then {
            refreshFlow.tryEmit(
                DataRefreshStatus.Finished(
                    HomeDataInteractor.Output.Success
                )
            )
        }

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        presenter.searchTextChange("second")
        presenter.refreshSwipe()


        verify(view, times(2)).hideAddButton()
        verify(view).hideRefreshProgress()
        verify(view, times(3)).showItems(any(), any(), any(), any(), any(), any(), any(), any())
        verify(view, times(1)).showAddButton()
    }

    @Test
    fun `empty view should be displayed when search is empty`() = runTest {
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )
        mockAccountData(null)
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        presenter.searchTextChange("empty search")

        verify(view).hideAddButton()
        verify(view).showSearchEmptyList()
        verify(view).showAddButton()
    }

    @Test
    fun `empty view should be displayed when there are no resources`() = runTest {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view, times(2)).showEmptyList()
    }

    @Test
    fun `error should be displayed when request failures`() = runTest {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Failure(AuthenticationState.Authenticated)))
        )
        mockAccountData(null)
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view).hideBackArrow()
        verify(view).showHomeScreenTitle(HomeDisplayViewModel.AllItems)
        verify(view).showAllItemsSearchHint()
        verify(view).hideAddButton()
        verify(view).hideRefreshProgress()
        verify(view).showDataRefreshError()
        verify(view).displaySearchAvatar(null)
        verify(view).hideFolderMoreMenuIcon()
        verify(view, never()).showAddButton()
    }

    @Test
    fun `error during refresh clicked should show correct ui`() = runTest {
        val refreshFlow =
            MutableStateFlow(
                DataRefreshStatus.Finished(
                    HomeDataInteractor.Output.Failure(AuthenticationState.Authenticated)
                )
            )
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(refreshFlow)
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        whenever(mockFullDataRefreshExecutor.performFullDataRefresh()).then {
            refreshFlow.tryEmit(
                DataRefreshStatus.Finished(
                    HomeDataInteractor.Output.Failure(AuthenticationState.Authenticated)
                )
            )
        }
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view).hideBackArrow()
        verify(view).hideAddButton()
        verify(view).hideRefreshProgress()
        verify(view).showDataRefreshError()
    }

    @Test
    fun `item clicked should open details screen`() = runTest {
        whenever(resourcesInteractor.fetchAndSaveResources()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        val model = ResourceModel(
            "id",
            "resTypeId",
            "folderId",
            "title",
            "subtitle",
            "",
            "initials",
            "",
            "",
            ResourcePermission.READ,
            null,
            ZonedDateTime.now()
        )
        mockAccountData(null)
        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        reset(view)
        presenter.itemClick(model)
        verify(view).navigateToDetails(model)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `home should navigate to root when current folder cannot be retrieved`() {
        mockGetLocalResourcesAndFoldersUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesAndFoldersUseCase.Output.Failure
        }
        mockGetLocalFolderUseCase.stub {
            onBlocking { execute(any()) } doReturn com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase.Output(
                FolderModel("childId", "root", "child folder", false, ResourcePermission.UPDATE)
            )
        }
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Folders(Folder.Child("childId"), "child name", isActiveFolderShared = false),
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view, times(2)).navigateToRootHomeFromChildHome(HomeDisplayViewModel.folderRoot())
        verify(view).showAddButton()
    }

    @Test
    fun `view should show correct titles for child items`() {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Folders(Folder.Child("childId"), "folder name", isActiveFolderShared = false),
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Tags("id", "tag name", isActiveTagShared = false),
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Groups("id", "group name"),
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.RecentlyModified, hasPreviousEntry = false, false, shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.SharedWithMe, hasPreviousEntry = false, false, shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.OwnedByMe, hasPreviousEntry = false, false, shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Favourites, hasPreviousEntry = false, false, shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems, hasPreviousEntry = false, false, shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view).showChildFolderTitle("folder name", isShared = false)
        verify(view).showTagTitle("tag name", isShared = false)
        verify(view).showGroupTitle("group name")
        verify(view, times(5)).showHomeScreenTitle(any())
    }

    @Test
    fun `view should show back arrow when in child item`() {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Folders(Folder.Child("childId"), "folder name", isActiveFolderShared = false),
            hasPreviousEntry = true,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Tags("id", "tag name", isActiveTagShared = false),
            hasPreviousEntry = true,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Groups("id", "group name"),
            hasPreviousEntry = true,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view, times(3)).showBackArrow()
    }

    @Test
    fun `view should navigate to selected item correctly based on root or child item`() {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.Folders(Folder.Child("childId"), "folder name", isActiveFolderShared = false),
            hasPreviousEntry = true,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.tagsClick()
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.tagsRoot(),
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        presenter.foldersClick()

        verify(view).navigateToRootHomeFromChildHome(HomeDisplayViewModel.tagsRoot())
        verify(view).navigateRootHomeFromRootHome(HomeDisplayViewModel.folderRoot())
    }

    @Test
    fun `view root should user selected filter by default`() {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        whenever(mockGetHomeDisplayPrefsUseCase.execute(any())).doReturn(
            GetHomeDisplayViewPrefsUseCase.Output(
                lastUsedHomeView = HomeDisplayView.ALL_ITEMS,
                userSetHomeView = DefaultFilterModel.FOLDERS
            )
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            showSuggestedModel = ShowSuggestedModel.DoNotShow,
            homeDisplayView = null,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        argumentCaptor<HomeDisplayViewModel> {
            verify(view).showHomeScreenTitle(capture())
            assertThat(firstValue).isInstanceOf(HomeDisplayViewModel.Folders::class.java)
        }
    }

    @Test
    fun `view should apply visibility settings correct`() {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        whenever(mockGetHomeDisplayPrefsUseCase.execute(any())).doReturn(
            GetHomeDisplayViewPrefsUseCase.Output(
                lastUsedHomeView = HomeDisplayView.ALL_ITEMS,
                userSetHomeView = DefaultFilterModel.FOLDERS
            )
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.argsRetrieved(
            showSuggestedModel = ShowSuggestedModel.DoNotShow,
            homeDisplayView = null,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)

        verify(view, never()).showCloseButton()
        verify(view, never()).showFolderMoreMenuIcon()

        presenter.argsRetrieved(
            showSuggestedModel = ShowSuggestedModel.DoNotShow,
            homeDisplayView = HomeDisplayViewModel.Folders(Folder.Root),
            hasPreviousEntry = false,
            shouldShowCloseButton = true,
            shouldShowResourceMoreMenu = true
        )

        verify(view).showCloseButton()
    }

    private fun mockResourcesList() = listOf(
        ResourceModel(
            resourceId = "id1",
            resourceTypeId = "resTypeId",
            folderId = "folderId",
            name = "first name",
            url = "",
            username = "",
            icon = "",
            initials = "",
            description = "desc",
            permission = ResourcePermission.READ,
            favouriteId = null,
            modified = ZonedDateTime.now()
        ), ResourceModel(
            resourceId = "id2",
            resourceTypeId = "resTypeId",
            folderId = "folderId",
            name = "second name",
            url = "",
            username = "",
            icon = "",
            initials = "",
            description = "desc",
            permission = ResourcePermission.READ,
            favouriteId = null,
            modified = ZonedDateTime.now()
        )
    )

    private fun mockAccountData(avatarUrl: String?) {
        whenever(getSelectedAccountDataUseCase.execute(anyOrNull())).thenReturn(
            GetSelectedAccountDataUseCase.Output(
                firstName = "",
                lastName = "",
                email = "",
                avatarUrl = avatarUrl,
                url = "",
                serverId = "",
                label = "label"
            )
        )
    }
}
