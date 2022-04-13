package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.database.impl.folders.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcesUseCase
import com.passbolt.mobile.android.feature.home.screen.DataRefreshStatus
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.home.screen.interactor.HomeDataInteractor
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayView
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
        mockFoldersInteractor.stub {
            onBlocking { fetchAndSaveFolders() } doReturn FoldersInteractor.Output.Success
        }
        mockGetLocalResourcesAndFoldersUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesAndFoldersUseCase.Output.Success(
                emptyList(),
                emptyList()
            )
        }
    }

    @Test
    fun `user avatar should be displayed when provided`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        val url = "avatar_url"

        mockAccountData(url)

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        verify(view).displaySearchAvatar(eq(url))
    }

    @Test
    fun `search input end icon should switch correctly based on input`() {
        val url = "avatar_url"
        mockAccountData(url)
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.searchTextChange("abc")
        presenter.searchTextChange("")

        verify(view, times(2)).displaySearchAvatar(url)
        verify(view).displaySearchClearIcon()
    }

    @Test
    fun `all fetched resources should be displayed when empty search text`() = runBlockingTest {
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )

        mockAccountData(null)
        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )

        verify(view).showHomeScreenTitle(HomeDisplayView.AllItems)
        verify(view).hideBackArrow()
        verify(view).showProgress()
        verify(view).hideAddButton()
        verify(view).hideProgress()
        verify(view).showAddButton()
        verify(view).showItems(any(), any(), any(), any(), any(), any(), any())
        verify(view).displaySearchAvatar(null)
        verify(view).hideRefreshProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `refresh swiped should reload data with filter applied when search text entered`() = runBlockingTest {
        mockResourcesList()
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )
        mockAccountData(null)
        val refreshFlow =
            MutableStateFlow(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        whenever(view.performRefreshUsingRefreshExecutor()).then {
            refreshFlow.tryEmit(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        }

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.searchTextChange("second")
        presenter.refreshSwipe()


        verify(view).performRefreshUsingRefreshExecutor()
        verify(view, times(2)).hideAddButton()
        verify(view).hideRefreshProgress()
        verify(view, times(2)).showItems(any(), any(), any(), any(), any(), any(), any())
        verify(view).showAddButton()
    }

    @Test
    fun `empty view should be displayed when search is empty`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )
        mockAccountData(null)
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.searchTextChange("empty search")

        verify(view).hideAddButton()
        verify(view).showSearchEmptyList()
        verify(view).showAddButton()
    }

    @Test
    fun `empty view should be displayed when there are no resources`() = runBlockingTest {
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )
        mockAccountData(null)
        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )

        verify(view).hideBackArrow()
        verify(view).showHomeScreenTitle(HomeDisplayView.AllItems)
        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideAddButton()
        verify(view).showEmptyList()
        verify(view).showAddButton()
        verify(view).displaySearchAvatar(null)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `error should be displayed when request failures`() = runBlockingTest {
        val refreshFlow = flowOf(
            DataRefreshStatus.Finished(HomeDataInteractor.Output.Failure(AuthenticationState.Authenticated))
        )
        mockAccountData(null)
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )

        verify(view).hideBackArrow()
        verify(view).showHomeScreenTitle(HomeDisplayView.AllItems)
        verify(view).showProgress()
        verify(view).hideAddButton()
        verify(view).hideProgress()
        verify(view).hideRefreshProgress()
        verify(view).showError()
        verify(view).displaySearchAvatar(null)
        verify(view, never()).showAddButton()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `error during refresh clicked should show correct ui`() = runBlockingTest {
        val refreshFlow =
            MutableStateFlow(DataRefreshStatus.Finished(HomeDataInteractor.Output.Failure(AuthenticationState.Authenticated)))
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        whenever(view.performRefreshUsingRefreshExecutor()).then {
            refreshFlow.tryEmit(DataRefreshStatus.Finished(HomeDataInteractor.Output.Failure(AuthenticationState.Authenticated)))
        }
        mockAccountData(null)

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.refreshClick()

        verify(view).performRefreshUsingRefreshExecutor()
        verify(view).hideBackArrow()
        verify(view, times(2)).showProgress()
        verify(view).hideAddButton()
        verify(view, times(2)).hideProgress()
        verify(view, times(2)).hideRefreshProgress()
        verify(view, times(2)).showError()
    }

    @Test
    fun `item clicked should open details screen`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
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
            false,
            ZonedDateTime.now()
        )
        mockAccountData(null)
        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        reset(view)
        presenter.itemClick(model)
        verify(view).navigateToDetails(model)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `3 dots clicked should open more screen`() = runBlockingTest {
        val model = ResourceModel(
            resourceId = "id",
            resourceTypeId = "resTypeId",
            folderId = "folderId",
            name = "title",
            username = "subtitle",
            icon = null,
            initials = "T",
            url = "",
            description = "desc",
            permission = ResourcePermission.READ,
            isFavourite = false,
            modified = ZonedDateTime.now()
        )
        val menuModel = resourceMenuModelMapper.map(model)
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        mockAccountData(null)
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        reset(view)
        presenter.moreClick(model)

        verify(view).navigateToMore(menuModel)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `view should show decrypt error correct`() {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.DecryptFailure(RuntimeException())
            )
        }
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).showDecryptionFailure()
    }

    @Test
    fun `view should show fetch error correct`() {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.FetchFailure(RuntimeException())
            )
        }
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).showFetchFailure()
    }

    @Test
    fun `view should show auth when passphrase not in cache`() {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.Unauthorized
            )
        }
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).showAuth(AuthenticationState.Unauthenticated.Reason.Passphrase)
    }

    @Test
    fun `view should copy secret after successful decrypt`() = runBlockingTest {
        mockAccountData(null)
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.Success(DECRYPTED_SECRET)
            )
        }
        mockResourceTypeFactory.stub {
            onBlocking { getResourceTypeEnum(any()) }.doReturn(
                ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
            )
        }
        whenever(mockSecretParser.extractPassword(any(), any()))
            .doReturn(String(DECRYPTED_SECRET))
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).addToClipboard(HomePresenter.SECRET_LABEL, String(DECRYPTED_SECRET))
    }

    @Test
    fun `delete resource should show confirmation dialog, delete and show snackbar`() = runBlockingTest {
        mockAccountData(null)
        whenever(mockDeleteResourceUseCase.execute(any()))
            .thenReturn(DeleteResourceUseCase.Output.Success)
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.Success(DECRYPTED_SECRET)
            )
        }
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showResourceDeletedSnackbar(RESOURCE_MODEL.name)
    }

    @Test
    fun `delete resource should show error when there is deletion error`() = runBlockingTest {
        mockAccountData(null)
        whenever(mockDeleteResourceUseCase.execute(any()))
            .thenReturn(
                DeleteResourceUseCase.Output.Failure<String>(
                    NetworkResult.Failure.NetworkError(
                        RuntimeException(),
                        ""
                    )
                )
            )
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.Success(DECRYPTED_SECRET)
            )
        }
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.AllItems,
            hasPreviousEntry = false
        )
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showGeneralError()
    }

    @Test
    fun `home should navigate to root when current folder cannot be retrieved`() {
        mockGetLocalResourcesAndFoldersUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesAndFoldersUseCase.Output.Failure
        }
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        mockAccountData(null)

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.Folders(Folder.Child("childId"), "child name", isActiveFolderShared = false),
            hasPreviousEntry = false
        )

        verify(view).navigateToRootHomeFromChildHome(HomeDisplayView.folderRoot())
    }

    @Test
    fun `view should show correct titles for child items`() {
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        mockAccountData(null)

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.Folders(Folder.Child("childId"), "folder name", isActiveFolderShared = false),
            hasPreviousEntry = false
        )
        presenter.argsRetrieved(
            HomeDisplayView.Tags("id", "tag name", isActiveTagShared = false),
            hasPreviousEntry = false
        )
        presenter.argsRetrieved(
            HomeDisplayView.Groups("id", "group name"),
            hasPreviousEntry = false
        )
        presenter.argsRetrieved(HomeDisplayView.RecentlyModified, hasPreviousEntry = false)
        presenter.argsRetrieved(HomeDisplayView.SharedWithMe, hasPreviousEntry = false)
        presenter.argsRetrieved(HomeDisplayView.OwnedByMe, hasPreviousEntry = false)
        presenter.argsRetrieved(HomeDisplayView.Favourites, hasPreviousEntry = false)
        presenter.argsRetrieved(HomeDisplayView.AllItems, hasPreviousEntry = false)

        verify(view).showChildFolderTitle("folder name", isShared = false)
        verify(view).showTagTitle("tag name", isShared = false)
        verify(view).showGroupTitle("group name")
        verify(view, times(5)).showHomeScreenTitle(any())
    }

    @Test
    fun `view should show back arrow when in child item`() {
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        mockAccountData(null)

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.Folders(Folder.Child("childId"), "folder name", isActiveFolderShared = false),
            hasPreviousEntry = true
        )
        presenter.argsRetrieved(
            HomeDisplayView.Tags("id", "tag name", isActiveTagShared = false),
            hasPreviousEntry = true
        )
        presenter.argsRetrieved(
            HomeDisplayView.Groups("id", "group name"),
            hasPreviousEntry = true
        )

        verify(view, times(3)).showBackArrow()
    }

    @Test
    fun `view should navigate to selected item correctly based on root or child item`() {
        val refreshFlow = flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        mockAccountData(null)

        presenter.viewCreate(refreshFlow)
        presenter.attach(view)
        presenter.argsRetrieved(
            HomeDisplayView.Folders(Folder.Child("childId"), "folder name", isActiveFolderShared = false),
            hasPreviousEntry = true
        )
        presenter.tagsClick()
        presenter.argsRetrieved(
            HomeDisplayView.tagsRoot(),
            hasPreviousEntry = false
        )
        presenter.foldersClick()

        verify(view).navigateToRootHomeFromChildHome(HomeDisplayView.tagsRoot())
        verify(view).navigateRootHomeFromRootHome(HomeDisplayView.folderRoot())
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
            isFavourite = false,
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
            isFavourite = false,
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

    private companion object {
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val INITIALS = "NN"
        private const val URL = "https://www.passbolt.com"
        private const val ID = "id"
        private const val RESOURCE_TYPE_ID = "resTypeId"
        private const val FOLDER_ID = "folderId"
        private const val DESCRIPTION = "desc"

        private val RESOURCE_MODEL = ResourceModel(
            ID,
            RESOURCE_TYPE_ID,
            FOLDER_ID,
            NAME,
            USERNAME,
            null,
            INITIALS,
            URL,
            DESCRIPTION,
            ResourcePermission.READ,
            false,
            ZonedDateTime.now()
        )
        private val DECRYPTED_SECRET = "secret".toByteArray()
    }
}
