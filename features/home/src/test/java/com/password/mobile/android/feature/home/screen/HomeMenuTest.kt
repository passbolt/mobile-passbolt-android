package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertyActionResult
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID

@ExperimentalCoroutinesApi
class HomeMenuTest : KoinTest {

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
        whenever(mockGetSelectedAccountUseCase.execute(anyOrNull())).thenReturn(
            GetSelectedAccountUseCase.Output("id")
        )
        mockGetLocalResourcesAndFoldersUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesAndFoldersUseCase.Output.Success(
                emptyList(),
                emptyList()
            )
        }
        mockCreateResourceMoreMenuModelUseCase.stub {
            onBlocking { execute(any()) } doReturn CreateResourceMoreMenuModelUseCase.Output(resourceMenuModel)
        }
        mockResourcesInteractor.stub {
            onBlocking { fetchAndSaveResources() } doReturn ResourceInteractor.Output.Success
        }

        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        whenever(mockGetSelectedAccountDataUseCase.execute(anyOrNull())).thenReturn(
            GetSelectedAccountDataUseCase.Output(
                firstName = "",
                lastName = "",
                email = "",
                avatarUrl = "avatarUrl",
                url = "",
                serverId = "",
                label = "label",
                role = "user"
            )
        )

        RESOURCE_MODEL = ResourceModel(
            resourceId = ID,
            resourceTypeId = RESOURCE_TYPE_ID.toString(),
            folderId = FOLDER_ID,
            permission = ResourcePermission.READ,
            favouriteId = null,
            modified = ZonedDateTime.now(),
            expiry = null,
            metadataJsonModel = MetadataJsonModel(
                """
                    {
                        "name": "$NAME",
                        "uri": "$URL",
                        "username": "$USERNAME",
                        "description": "$DESCRIPTION"
                    }
                """.trimIndent()
            ),
            metadataKeyId = null,
            metadataKeyType = null
        )
    }

    @Test
    fun `3 dots clicked should open more screen`() = runTest {
        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        presenter.resourceMoreClick(RESOURCE_MODEL)

        verify(view).navigateToMore(RESOURCE_MODEL.resourceId, RESOURCE_MODEL.metadataJsonModel.name)
    }

    @Test
    fun `view should show decrypt error correct`() {
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { providePassword() } doReturn flowOf(SecretPropertyActionResult.DecryptionFailure())
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).showDecryptionFailure()
    }

    @Test
    fun `view should show fetch error correct`() {
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { providePassword() } doReturn flowOf(SecretPropertyActionResult.FetchFailure())
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).showFetchFailure()
    }

    @Test
    fun `view should copy secret after successful decrypt`() = runTest {
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordString.slug)
            )
        }
        val password = "pass"
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { providePassword() } doReturn flowOf(
                SecretPropertyActionResult.Success(
                    SecretPropertiesActionsInteractor.SECRET_LABEL,
                    isSecret = true,
                    password
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyPasswordClick()

        verify(view).addToClipboard(
            SecretPropertiesActionsInteractor.SECRET_LABEL,
            password,
            true
        )
    }

    @Test
    fun `view should copy description from secret after successful decrypt`() = runTest {
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordAndDescription.slug)
            )
        }
        val resourceDescription = "desc"
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { provideSecureNote() } doReturn flowOf(
                SecretPropertyActionResult.Success(
                    SecretPropertiesActionsInteractor.SECURE_NOTE_LABEL,
                    isSecret = true,
                    resourceDescription
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyDescriptionClick()

        verify(view).addToClipboard(
            SecretPropertiesActionsInteractor.SECURE_NOTE_LABEL,
            resourceDescription,
            isSecret = true
        )
    }

    @Test
    fun `view should copy description from resource`() = runTest {
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordString.slug)
            )
        }
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideDescription() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.DESCRIPTION_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.metadataJsonModel.description.orEmpty()
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyDescriptionClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.DESCRIPTION_LABEL,
            RESOURCE_MODEL.metadataJsonModel.description.orEmpty(),
            isSecret = false
        )
    }

    @Test
    fun `delete resource should show confirmation dialog, delete and show snackbar`() = runTest {
        mockResourceCommonActionsInteractor.stub {
            onBlocking { deleteResource() } doReturn flowOf(
                ResourceCommonActionResult.Success(RESOURCE_MODEL.metadataJsonModel.name)
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showResourceDeletedSnackbar(RESOURCE_MODEL.metadataJsonModel.name)
    }

    @Test
    fun `delete resource should show error when there is deletion error`() = runTest {
        mockResourceCommonActionsInteractor.stub {
            onBlocking { deleteResource() } doReturn flowOf(
                ResourceCommonActionResult.Failure
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showDeleteResourceFailure()
    }

    @Test
    fun `add resource to favourites should show failure correct`() = runTest {
        mockResourceCommonActionsInteractor.stub {
            onBlocking { toggleFavourite(ADD_TO_FAVOURITES) } doReturn flowOf(
                ResourceCommonActionResult.Failure
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuFavouriteClick(ADD_TO_FAVOURITES)

        verify(view).showToggleFavouriteFailure()
    }

    @Test
    fun `resource url should be copied correct`() = runTest {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideWebsiteUrl() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.URL_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.metadataJsonModel.uri.orEmpty()
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyUrlClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.URL_LABEL,
            RESOURCE_MODEL.metadataJsonModel.uri.orEmpty(),
            isSecret = false
        )
    }

    @Test
    fun `resource username should be copied correct`() = runTest {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideUsername() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.USERNAME_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.metadataJsonModel.username.orEmpty()
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuCopyUsernameClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.USERNAME_LABEL,
            RESOURCE_MODEL.metadataJsonModel.username.orEmpty(),
            isSecret = false
        )
    }

    @Test
    fun `resource url website should be opened`() = runTest {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideWebsiteUrl() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.URL_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.metadataJsonModel.uri.orEmpty()
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
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuLaunchWebsiteClick()

        verify(view).openWebsite(RESOURCE_MODEL.metadataJsonModel.uri.orEmpty())
    }

    @Test
    fun `edit password should navigate to password edit form`() = runTest {
        presenter.attach(view)
        presenter.argsRetrieved(
            ShowSuggestedModel.DoNotShow,
            HomeDisplayViewModel.AllItems,
            hasPreviousEntry = false,
            shouldShowCloseButton = false,
            shouldShowResourceMoreMenu = false
        )
        presenter.resume(view)
        presenter.resourceMoreClick(RESOURCE_MODEL)
        presenter.menuEditClick()

        verify(view).navigateToEdit(RESOURCE_MODEL)
    }

    private companion object {
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val URL = "https://www.passbolt.com"
        private const val ID = "id"
        private val RESOURCE_TYPE_ID = UUID.randomUUID()
        private const val FOLDER_ID = "folderId"
        private const val DESCRIPTION = "desc"

        val resourceMenuModel = ResourceMoreMenuModel(
            title = "title",
            canCopy = true,
            canDelete = true,
            canEdit = true,
            canShare = true,
            favouriteOption = ADD_TO_FAVOURITES
        )

        private lateinit var RESOURCE_MODEL: ResourceModel
    }
}
