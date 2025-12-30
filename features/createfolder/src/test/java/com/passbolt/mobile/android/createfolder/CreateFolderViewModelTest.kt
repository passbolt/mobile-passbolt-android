package com.passbolt.mobile.android.createfolder

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.AddLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.CreateFolderUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.FolderShareInteractor
import com.passbolt.mobile.android.core.commonfolders.usecase.db.AddLocalFolderUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.FolderNameChanged
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.GoBack
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.Initialize
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.Save
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.FolderCreated
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.NavigateUp
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.createfolder.CreateFolderValidationError.MaxLengthExceeded
import com.passbolt.mobile.android.createfolder.CreateFolderViewModel.Companion.FOLDER_NAME_MAX_LENGTH
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase.Output.JwtWillExpire
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.FolderModelWithAttributes
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class CreateFolderViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetLocalFolderLocationUseCase>() }
                        single { mock<GetLocalFolderPermissionsUseCase>() }
                        single { mock<CreateFolderUseCase>() }
                        single { mock<GetLocalFolderDetailsUseCase>() }
                        single { mock<GetLocalParentFolderPermissionsToApplyToNewItemUseCase>() }
                        single { mock<FolderShareInteractor>() }
                        single { mock<AddLocalFolderUseCase>() }
                        single { mock<AddLocalFolderPermissionsUseCase>() }
                        single { mock<GetLocalCurrentUserUseCase>() }
                        single { mock<UsersModelMapper>() }
                        single { mock<GetSessionExpiryUseCase>() }
                        single { mock<PassphraseMemoryCache>() }
                        single { CreateFolderIdlingResource() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::CreateFolderViewModel)
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CreateFolderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val passphraseMemoryCache: PassphraseMemoryCache = get()
        whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

        val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
        whenever(getSessionExpiryUseCase.execute(Unit)) doReturn JwtWillExpire(ZonedDateTime.now().plusMinutes(5))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should initialize with empty state when no parent folder`() =
        runTest {
            setupMocksForRootFolder()

            viewModel = get()
            viewModel.onIntent(Initialize(parentFolderId = null))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.parentFolderId).isNull()
                assertThat(state.folderName).isEmpty()
                assertThat(state.locationPath).isEmpty()
                assertThat(state.permissions).hasSize(1)
                assertThat(state.isLoading).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should initialize with parent folder data`() =
        runTest {
            setupMocksForParentFolder()

            viewModel = get()
            viewModel.onIntent(Initialize(PARENT_FOLDER_ID))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.parentFolderId).isEqualTo(PARENT_FOLDER_ID)
                assertThat(state.locationPath).containsExactly("Parent Folder")
                assertThat(state.permissions).hasSize(2)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should update folder name when FolderNameChanged intent`() =
        runTest {
            setupMocksForRootFolder()

            viewModel = get()
            viewModel.onIntent(FolderNameChanged("New Folder"))

            viewModel.viewState.test {
                val changedNameState = awaitItem()
                assertThat(changedNameState.folderName).isEqualTo("New Folder")
                assertThat(changedNameState.folderNameValidationErrors).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should navigate up when GoBack intent`() =
        runTest {
            setupMocksForRootFolder()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                assertIs<NavigateUp>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show validation error when folder name exceeds max length`() =
        runTest {
            setupMocksForRootFolder()

            viewModel = get()
            val tooLongName = "a".repeat(FOLDER_NAME_MAX_LENGTH + 1)
            viewModel.onIntent(FolderNameChanged(tooLongName))

            viewModel.viewState.test {
                assertThat(awaitItem().folderName).isEqualTo(tooLongName)

                viewModel.onIntent(Save)

                val validated = awaitItem()
                assertThat(validated.folderNameValidationErrors).contains(MaxLengthExceeded(FOLDER_NAME_MAX_LENGTH))
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show validation error when folder name is blank`() =
        runTest {
            setupMocksForRootFolder()

            viewModel = get()
            viewModel.onIntent(FolderNameChanged(" "))

            viewModel.viewState.test {
                assertThat(awaitItem().folderName).isEqualTo(" ")

                viewModel.onIntent(Save)

                val state = awaitItem()
                assertThat(state.folderNameValidationErrors).isNotEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should create folder successfully in root and emit FolderCreated`() =
        runTest {
            setupMocksForRootFolder()
            setupSuccessfulFolderCreation()

            viewModel = get()
            viewModel.onIntent(Initialize(parentFolderId = null))
            viewModel.onIntent(FolderNameChanged("Test Folder"))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val sideEffect = awaitItem()
                assertIs<FolderCreated>(sideEffect)
                assertThat(sideEffect.folderName).isEqualTo("Test Folder")
            }

            verify(get<CreateFolderUseCase>()).execute(any())
            verify(get<AddLocalFolderUseCase>()).execute(any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show error snackbar when folder creation fails`() =
        runTest {
            setupMocksForRootFolder()
            setupFailedFolderCreation()

            viewModel = get()
            viewModel.onIntent(Initialize(null))
            viewModel.onIntent(FolderNameChanged("Test Folder"))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val sideEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(SnackbarErrorType.CREATE_FOLDER_ERROR)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should set loading state during folder creation`() =
        runTest {
            setupMocksForRootFolder()
            setupSuccessfulFolderCreation()

            viewModel = get()
            viewModel.onIntent(Initialize(null))
            viewModel.onIntent(FolderNameChanged("Test Folder"))

            viewModel.viewState.test {
                awaitItem() // clear validation state

                viewModel.onIntent(Save)

                assertThat(awaitItem().isLoading).isTrue()
                assertThat(awaitItem().isLoading).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should create folder in shared parent folder and apply permissions`() =
        runTest {
            setupMocksForSharedParentFolder()
            setupSuccessfulFolderCreation()
            setupSuccessfulFolderShare()

            viewModel = get()
            viewModel.onIntent(Initialize(PARENT_FOLDER_ID))
            viewModel.onIntent(FolderNameChanged("Test Folder"))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val sideEffect = awaitItem()
                assertIs<FolderCreated>(sideEffect)
            }

            verify(get<CreateFolderUseCase>()).execute(any())
            verify(get<FolderShareInteractor>()).shareFolder(any(), any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should create folder in non-shared parent folder without applying permissions`() =
        runTest {
            setupMocksForNonSharedParentFolder()
            setupSuccessfulFolderCreation()

            viewModel = get()
            viewModel.onIntent(Initialize(PARENT_FOLDER_ID))

            viewModel.onIntent(FolderNameChanged("Test Folder"))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val sideEffect = awaitItem()
                assertIs<FolderCreated>(sideEffect)
            }

            verify(get<CreateFolderUseCase>()).execute(any())
            verify(get<FolderShareInteractor>(), never()).shareFolder(any(), any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show error when sharing folder fails`() =
        runTest {
            setupMocksForSharedParentFolder()
            setupSuccessfulFolderCreation()
            setupFailedFolderShare()

            viewModel = get()
            viewModel.onIntent(Initialize(PARENT_FOLDER_ID))

            viewModel.onIntent(FolderNameChanged("Test Folder"))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val sideEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(SnackbarErrorType.SHARE_FOLDER_ERROR)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should clear validation errors when folder name changes`() =
        runTest {
            setupMocksForRootFolder()

            viewModel = get()
            viewModel.onIntent(FolderNameChanged(""))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Save)
                val errorState = awaitItem()
                assertThat(errorState.folderNameValidationErrors).isNotEmpty()

                viewModel.onIntent(FolderNameChanged("Valid Name"))
                val clearedState = awaitItem()
                assertThat(clearedState.folderNameValidationErrors).isEmpty()
            }
        }

    private suspend fun setupMocksForRootFolder() {
        val getCurrentUserUseCase = get<GetLocalCurrentUserUseCase>()
        whenever(getCurrentUserUseCase.execute(Unit)) doReturn GetLocalCurrentUserUseCase.Output(mockUserModel)

        val usersModelMapper = get<UsersModelMapper>()
        whenever(usersModelMapper.mapToUserWithAvatar(mockUserModel)) doReturn mockUserWithAvatar
    }

    private suspend fun setupMocksForParentFolder() {
        setupMocksForRootFolder()

        val getLocationUseCase = get<GetLocalFolderLocationUseCase>()
        whenever(getLocationUseCase.execute(any())) doReturn
            GetLocalFolderLocationUseCase.Output(
                listOf(mockParentFolderModel),
            )

        val getPermissionsUseCase = get<GetLocalFolderPermissionsUseCase>()
        whenever(getPermissionsUseCase.execute(any())) doReturn
            GetLocalFolderPermissionsUseCase.Output(
                listOf(mockPermission1, mockPermission2),
            )
    }

    private suspend fun setupMocksForSharedParentFolder() {
        setupMocksForParentFolder()

        val getFolderDetailsUseCase = get<GetLocalFolderDetailsUseCase>()
        whenever(getFolderDetailsUseCase.execute(any())) doReturn
            GetLocalFolderDetailsUseCase.Output(
                folder = mockParentFolderModel.copy(isShared = true),
            )

        val getPermissionsToCopy = get<GetLocalParentFolderPermissionsToApplyToNewItemUseCase>()
        whenever(getPermissionsToCopy.execute(any())) doReturn
            GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Output(
                listOf(mockPermission1, mockPermission2),
            )
    }

    private suspend fun setupMocksForNonSharedParentFolder() {
        setupMocksForParentFolder()

        val getFolderDetailsUseCase = get<GetLocalFolderDetailsUseCase>()
        whenever(getFolderDetailsUseCase.execute(any())) doReturn
            GetLocalFolderDetailsUseCase.Output(
                folder = mockParentFolderModel.copy(isShared = false),
            )
    }

    private suspend fun setupSuccessfulFolderCreation() {
        val createFolderUseCase = get<CreateFolderUseCase>()
        val addLocalFolderUseCase = get<AddLocalFolderUseCase>()
        val addPermissionsUseCase = get<AddLocalFolderPermissionsUseCase>()

        whenever(createFolderUseCase.execute(any())) doReturn
            CreateFolderUseCase.Output.Success(
                mockFolderWithAttributes,
            )

        whenever(addLocalFolderUseCase.execute(any())) doReturn Unit
        whenever(addPermissionsUseCase.execute(any())) doReturn Unit
    }

    private suspend fun setupFailedFolderCreation() {
        val createFolderUseCase = get<CreateFolderUseCase>()

        whenever(createFolderUseCase.execute(any())) doReturn
            CreateFolderUseCase.Output.Failure(
                NetworkResult.Failure.NetworkError<Any>(Exception("Network error"), "Network error"),
            )
    }

    private suspend fun setupSuccessfulFolderShare() {
        val folderShareInteractor = get<FolderShareInteractor>()

        whenever(folderShareInteractor.shareFolder(any(), any())) doReturn FolderShareInteractor.Output.Success
    }

    private suspend fun setupFailedFolderShare() {
        val folderShareInteractor = get<FolderShareInteractor>()

        whenever(folderShareInteractor.shareFolder(any(), any())) doReturn
            FolderShareInteractor.Output.ShareFailure(
                Exception("Share failed"),
            )
    }

    private companion object {
        private const val PARENT_FOLDER_ID = "parent-folder-id"
        private const val NEW_FOLDER_ID = "new-folder-id"
        private const val USER_ID = "user-id"

        private val mockUserModel =
            mock<UserModel>()

        private val mockUserWithAvatar =
            UserWithAvatar(
                userId = USER_ID,
                firstName = "Test",
                lastName = "User",
                userName = "test@example.com",
                isDisabled = false,
                avatarUrl = null,
            )

        private val mockPermission1 =
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.OWNER,
                permissionId = "perm-1",
                user = mockUserWithAvatar,
            )

        private val mockPermission2 =
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "perm-2",
                user = mockUserWithAvatar,
            )

        private val mockPermissionModel1 =
            PermissionModel.UserPermissionModel(
                permission = ResourcePermission.OWNER,
                permissionId = "perm-1",
                userId = USER_ID,
            )

        private val mockParentFolderModel =
            FolderModel(
                folderId = PARENT_FOLDER_ID,
                parentFolderId = null,
                name = "Parent Folder",
                isShared = false,
                permission = ResourcePermission.OWNER,
            )

        private val mockNewFolderModel =
            FolderModel(
                folderId = NEW_FOLDER_ID,
                parentFolderId = PARENT_FOLDER_ID,
                name = "Test Folder",
                isShared = false,
                permission = ResourcePermission.OWNER,
            )

        private val mockFolderWithAttributes =
            FolderModelWithAttributes(
                folderModel = mockNewFolderModel,
                folderPermissions = listOf(mockPermissionModel1),
            )
    }
}
