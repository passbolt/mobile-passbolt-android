package com.password.mobile.android.feature.home

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.mvp.session.AuthenticationState
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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
    }

    @Test
    fun `user avatar should be displayed when provided`() = runBlockingTest {
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success(emptyList(), emptyList())
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
        val mockedList = mockResourcesList()
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success(mockedList, emptyList())
        )
        mockAccountData(null)
        presenter.attach(view)

        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showPasswords(anyOrNull())
        verify(view).displaySearchAvatar(null)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `refresh swiped should reload data with filter applied when search text entered`() = runBlockingTest {
        val mockedList = mockResourcesList()
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success(mockedList, emptyList())
        )
        mockAccountData(null)
        presenter.attach(view)
        presenter.searchTextChange("second")
        reset(view)
        presenter.refreshSwipe()

        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showPasswords(anyOrNull())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `empty view should be displayed when search is empty`() = runBlockingTest {
        val mockedList = mockResourcesList()
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success(mockedList, emptyList())
        )
        mockAccountData(null)

        presenter.attach(view)
        presenter.searchTextChange("third")
        reset(view)
        presenter.refreshSwipe()

        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showSearchEmptyList()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `empty view should be displayed when there are no resources`() = runBlockingTest {
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Success(emptyList(), emptyList())
        )
        mockAccountData(null)
        presenter.attach(view)

        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showEmptyList()
        verify(view).displaySearchAvatar(null)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `error should be displayed when request failures`() = runBlockingTest {
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        mockAccountData(null)
        presenter.attach(view)
        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showError()
        verify(view).displaySearchAvatar(null)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `refresh clicked should fetch resources`() = runBlockingTest {
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        mockAccountData(null)
        presenter.attach(view)
        reset(view)
        presenter.refreshClick()

        verify(view).showProgress()
        verify(view).hideRefreshProgress()
        verify(view).hideProgress()
        verify(view).showError()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `item clicked should open details screen`() = runBlockingTest {
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        val model = ResourceModel(
            "id",
            "resTypeId",
            "title",
            "subtitle",
            "",
            "initials",
            "",
            ""
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
            name = "title",
            username = "subtitle",
            icon = null,
            initials = "T",
            url = "",
            description = "desc"
        )
        whenever(resourcesInteractor.fetchResourcesWithTypes()).thenReturn(
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)
        )
        mockAccountData(null)
        presenter.attach(view)
        reset(view)
        presenter.moreClick(model)
        verify(view).navigateToMore(model)
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

    private fun mockResourcesList() = listOf(
        ResourceModel(
            resourceId = "id1",
            resourceTypeId = "resTypeId",
            name = "first name",
            url = "",
            username = "",
            icon = "",
            initials = "",
            description = "desc",
        ), ResourceModel(
            resourceId = "id2",
            resourceTypeId = "resTypeId",
            name = "second name",
            url = "",
            username = "",
            icon = "",
            initials = "",
            description = "desc"
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
                serverId = ""
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
        private const val DESCRIPTION = "desc"

        private val RESOURCE_MODEL = ResourceModel(
            ID,
            RESOURCE_TYPE_ID,
            NAME,
            USERNAME,
            null,
            INITIALS,
            URL,
            DESCRIPTION
        )
        private val DECRYPTED_SECRET = "secret".toByteArray()
    }
}
