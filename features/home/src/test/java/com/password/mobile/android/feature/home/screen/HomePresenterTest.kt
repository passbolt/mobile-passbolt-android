package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourcesDisplayView
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    @Test
    fun `user avatar should be displayed when provided`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        val url = "avatar_url"
        mockAccountData(url)
        presenter.attach(view)
        verify(view).displaySearchAvatar(eq(url))
    }

    @Test
    fun `search input end icon should switch correctly based on input`() {
        val url = "avatar_url"
        mockAccountData(url)

        presenter.attach(view)
        presenter.searchTextChange("abc")
        presenter.searchTextChange("")

        verify(view, times(2)).displaySearchAvatar(url)
        verify(view).displaySearchClearIcon()
    }

    @Test
    fun `all fetched resources should be displayed when empty search text`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(mockResourcesList())
        )

        mockAccountData(null)
        presenter.attach(view)

        verify(view).showHomeScreenTitle(ResourcesDisplayView.ALL)
        verify(view).showProgress()
        verify(view).hideUpdateButton()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showAddButton()
        verify(view).showPasswords(anyOrNull())
        verify(view).displaySearchAvatar(null)
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

        presenter.attach(view)
        presenter.searchTextChange("second")
        reset(view)
        presenter.refreshSwipe()

        verify(view).hideUpdateButton()
        verify(view).hideRefreshProgress()
        verify(view).showPasswords(anyOrNull())
        verify(view).showAddButton()
        verifyNoMoreInteractions(view)
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

        presenter.attach(view)
        presenter.searchTextChange("third")
        reset(view)
        presenter.refreshSwipe()

        verify(view).hideUpdateButton()
        verify(view).hideRefreshProgress()
        verify(view).showSearchEmptyList()
        verify(view).showAddButton()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `empty view should be displayed when there are no resources`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success
        )
        whenever(mockGetLocalResourcesUseCase.execute(any())).thenReturn(
            GetLocalResourcesUseCase.Output(emptyList())
        )
        mockAccountData(null)
        presenter.attach(view)

        verify(view).showHomeScreenTitle(ResourcesDisplayView.ALL)
        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).hideUpdateButton()
        verify(view).showEmptyList()
        verify(view).showAddButton()
        verify(view).displaySearchAvatar(null)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `error should be displayed when request failures`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        mockAccountData(null)

        presenter.attach(view)

        verify(view).showHomeScreenTitle(ResourcesDisplayView.ALL)
        verify(view).showProgress()
        verify(view).hideUpdateButton()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showError()
        verify(view).displaySearchAvatar(null)
        verify(view, never()).showAddButton()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `error during refresh clicked should show correct ui`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        mockAccountData(null)
        presenter.attach(view)
        reset(view)
        presenter.refreshClick()

        verify(view).showProgress()
        verify(view).hideUpdateButton()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showError()
        verify(view, never()).showAddButton()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `item clicked should open details screen`() = runBlockingTest {
        whenever(resourcesInteractor.updateResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
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
            false,
            ZonedDateTime.now()
        )
        mockAccountData(null)
        presenter.attach(view)
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

        presenter.attach(view)
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

        presenter.attach(view)
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

        presenter.attach(view)
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

        presenter.attach(view)
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

        presenter.attach(view)
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

        presenter.attach(view)
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

        presenter.attach(view)
        presenter.moreClick(RESOURCE_MODEL)
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showGeneralError()
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
