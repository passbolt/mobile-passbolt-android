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

package com.passbolt.mobile.android.feature.home.screen

import androidx.paging.PagingData
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.autofill.DetectAutofillConflict
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.commontest.session.validSessionTestModule
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.domain.accounts.AccountSwitchFlow
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.preferences.usecase.GetHomeDisplayViewPreferencesUseCase
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.domain.secrets.usecase.offline.GetOfflineCacheStatusUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.MarkResourceOfflineUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.UnmarkResourceOfflineUseCase
import com.passbolt.mobile.android.domain.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.domain.users.profile.UserProfileRefreshTrackingFlow
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateFolder
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateNote
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreatePassword
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Initialize
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OnResume
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Search
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateFolder
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel.DoNotShow
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.PROFILE_FETCH_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.FOLDER_CREATED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_DELETED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_SHARED
import com.passbolt.mobile.android.feature.home.screen.data.HomeData
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataProvider
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.ui.DefaultFilterUiModel
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.NotLoaded
import com.passbolt.mobile.android.ui.HomeDisplayViewPreferencesUiModel
import com.passbolt.mobile.android.ui.HomeDisplayViewUiModel
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.OfflineModeSetting
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourceUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::DataRefreshTrackingFlow)
                    singleOf(::SessionRefreshTrackingFlow)
                    single { mock<GetSelectedAccountDataUseCase>() }
                    single { mock<GetSelectedAccountUseCase> { on { execute(Unit) } doReturn GetSelectedAccountUseCase.Output("userId") } }
                    single { mock<GetHomeDisplayViewPreferencesUseCase>() }
                    single { mock<HomeDataProvider>() }
                    single { mock<GetLocalFolderDetailsUseCase>() }
                    single { mock<ResourceAccessInteractor>() }
                    single { mock<DetectAutofillConflict>() }
                    singleOf(::OfflineSessionState)
                    single { mock<MarkResourceOfflineUseCase>() }
                    single { mock<UnmarkResourceOfflineUseCase>() }
                    single {
                        mock<GetOfflineCacheStatusUseCase> {
                            onBlocking { execute(Unit) } doReturn
                                GetOfflineCacheStatusUseCase.Output(
                                    mode = OfflineModeSetting.OFF,
                                    lastSyncEpochMillis = null,
                                    cachedCount = 0,
                                    markedCount = 0,
                                    isOfflineSession = false,
                                )
                        }
                    }
                    single {
                        mock<UserProfileInteractor> {
                            onBlocking { fetchAndUpdateUserProfile() } doReturn UserProfileInteractor.Output.Success
                        }
                    }
                    singleOf(::UserProfileRefreshTrackingFlow)
                    single { AccountSwitchFlow(mock { on { execute(any()) } doReturn GetSelectedAccountUseCase.Output("id1") }) }
                    single(named(JSON_MODEL_GSON)) { GsonBuilder().serializeNulls().create() }
                    single {
                        Configuration
                            .builder()
                            .jsonProvider(GsonJsonProvider())
                            .mappingProvider(GsonMappingProvider())
                            .options(EnumSet.noneOf(Option::class.java))
                            .build()
                    }
                    singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                    factoryOf(::HomeViewModel)
                },
                validSessionTestModule,
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
            GetSelectedAccountDataUseCase.Output(
                firstName = "First",
                lastName = "Last",
                email = "first@passbolt.com",
                avatarUrl = "www.passbolt.com/avatar.png",
                url = "www.passbolt.com",
                serverId = "1",
                label = "label",
                role = "user",
            ),
        )

        whenever(get<GetHomeDisplayViewPreferencesUseCase>().execute(Unit)).thenReturn(
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = HomeDisplayViewUiModel.ALL_ITEMS,
                userSetHomeView = DefaultFilterUiModel.ALL_ITEMS,
            ),
        )

        get<HomeDataProvider>().stub {
            onBlocking {
                provideData(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(HomeData())
        }

        get<ResourceAccessInteractor>().stub {
            onBlocking { canCreateResource(anyOrNull()) }.doReturn(true)
            onBlocking { canShareResource() }.doReturn(true)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should show user avatar on init`() =
        runTest {
            val avatar = "avatar_url"
            whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
                GetSelectedAccountDataUseCase.Output(
                    firstName = "First",
                    lastName = "Last",
                    email = "user@example.com",
                    avatarUrl = avatar,
                    url = "https://passbolt.com",
                    serverId = "server1",
                    label = "Server Label",
                    role = "user",
                ),
            )

            viewModel = get()

            assertThat(viewModel.viewState.value.userAvatar).isEqualTo(avatar)
        }

    @Test
    fun `should show error snackbar when profile refresh fails on init`() =
        runTest {
            val errorMessage = "profile fetch failed"
            get<UserProfileInteractor>().stub {
                onBlocking { fetchAndUpdateUserProfile() } doReturn
                    UserProfileInteractor.Output.Failure(DomainResult.Incomplete.Error(UNKNOWN, errorMessage))
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(PROFILE_FETCH_FAILURE)
                assertThat(effect.message).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `should update search state when search query changes`() =
        runTest {
            val mockHomeData = mockResourcesData()
            whenever(get<HomeDataProvider>().provideData(any(), any(), any(), any())).thenReturn(mockHomeData)

            viewModel = get()
            viewModel.onIntent(Search("test query"))

            viewModel.viewState.drop(1).test {
                val updatedState = awaitItem()
                assertThat(updatedState.searchQuery).isEqualTo("test query")
                assertThat(updatedState.searchInputEndIconMode).isEqualTo(CLEAR)
                assertThat(updatedState.homeData).isEqualTo(mockHomeData)
            }
        }

    @Test
    fun `should clear search and reset icon mode when search cleared`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Search("test query"))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SearchEndIconAction)
                val updatedState = awaitItem()
                assertThat(updatedState.searchQuery).isEmpty()
                assertThat(updatedState.searchInputEndIconMode).isEqualTo(AVATAR)
            }
        }

    @Test
    fun `should show and close account switcher when requested`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SearchEndIconAction)
                assertThat(awaitItem().showAccountSwitchBottomSheet).isTrue()
                viewModel.onIntent(CloseSwitchAccount)
                assertThat(awaitItem().showAccountSwitchBottomSheet).isFalse()
            }
        }

    @Test
    fun `should show and hide create resource menu when requested`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(OpenCreateResourceMenu)
                assertThat(awaitItem().showCreateResourceBottomSheet).isTrue()
                viewModel.onIntent(CloseCreateResourceMenu)
                assertThat(awaitItem().showCreateResourceBottomSheet).isFalse()
            }
        }

    @Test
    fun `should navigate to correct create resource form when creating resource`() =
        runTest {
            mockCanCreateResource(true)
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val createPasswordEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createPasswordEffect)
                assertThat(createPasswordEffect.leadingContentType).isEqualTo(PASSWORD)
                assertThat(createPasswordEffect.folderId).isNull()

                viewModel.onIntent(CreateNote)
                val createNoteEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createNoteEffect)
                assertThat(createNoteEffect.leadingContentType).isEqualTo(STANDALONE_NOTE)
                assertThat(createNoteEffect.folderId).isNull()

                viewModel.onIntent(CreateTotp)
                val createTotpEffect = awaitItem()
                assertIs<NavigateToCreateTotp>(createTotpEffect)
                assertThat(createTotpEffect.folderId).isNull()

                viewModel.onIntent(CreateFolder)
                val createFolderEffect = awaitItem()
                assertIs<NavigateToCreateFolder>(createFolderEffect)
                assertThat(createFolderEffect.folderId).isNull()
            }
        }

    @Test
    fun `should navigate to correct create resource form when creating resource in child folder`() =
        runTest {
            mockCanCreateResource(true)
            viewModel = get()

            viewModel.onIntent(
                HomeIntent.ShowHomeView(
                    HomeDisplayViewModel.Folders(
                        Folder.Child("folderId"),
                    ),
                ),
            )
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val createPasswordEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createPasswordEffect)
                assertThat(createPasswordEffect.leadingContentType).isEqualTo(PASSWORD)
                assertThat(createPasswordEffect.folderId).isEqualTo("folderId")

                viewModel.onIntent(CreateNote)
                val createNoteEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createNoteEffect)
                assertThat(createNoteEffect.leadingContentType).isEqualTo(STANDALONE_NOTE)
                assertThat(createNoteEffect.folderId).isEqualTo("folderId")

                viewModel.onIntent(CreateTotp)
                val createTotpEffect = awaitItem()
                assertIs<NavigateToCreateTotp>(createTotpEffect)
                assertThat(createTotpEffect.folderId).isEqualTo("folderId")

                viewModel.onIntent(CreateFolder)
                val createFolderEffect = awaitItem()
                assertIs<NavigateToCreateFolder>(createFolderEffect)
                assertThat(createFolderEffect.folderId).isEqualTo("folderId")
            }
        }

    @Test
    fun `should show error when cannot create resource`() =
        runTest {
            mockCanCreateResource(false)
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val createPasswordEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(createPasswordEffect)
                assertThat(createPasswordEffect.type).isEqualTo(NO_SHARED_KEY_ACCESS)

                viewModel.onIntent(CreateNote)
                val createNoteEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(createNoteEffect)
                assertThat(createNoteEffect.type).isEqualTo(NO_SHARED_KEY_ACCESS)

                viewModel.onIntent(CreateTotp)
                val createTotpEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(createTotpEffect)
                assertThat(createTotpEffect.type).isEqualTo(NO_SHARED_KEY_ACCESS)

                viewModel.onIntent(CreateFolder)
                val createFolderEffect = awaitItem()
                assertIs<NavigateToCreateFolder>(createFolderEffect)
                assertThat(createFolderEffect.folderId).isEqualTo(null)
            }
        }

    @Test
    fun `should update state during data refresh`() =
        runTest {
            mockHomeData()
            val dataRefreshFlow: DataRefreshTrackingFlow = get()

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))

            viewModel.viewState.drop(2).test {
                dataRefreshFlow.updateStatus(InProgress(progress = 0f))
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()
                assertThat(inProgress.canCreateResource).isFalse()

                dataRefreshFlow.updateStatus(FinishedWithSuccess)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()
                assertThat(finished.canCreateResource).isTrue()
            }
        }

    @Test
    fun `should show create button from local data without refresh in autofill context`() =
        runTest {
            mockHomeData()
            mockCanCreateResource(true)

            viewModel = get()

            viewModel.viewState.test {
                viewModel.onIntent(Initialize(DoNotShow, NotLoaded, appContext = AppContext.AUTOFILL))

                var state = awaitItem()
                while (!state.canCreateResource) {
                    state = awaitItem()
                }
                assertThat(state.isRefreshing).isFalse()
            }
        }

    @Test
    fun `should show error on refresh failure`() =
        runTest {
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            mockHomeData()
            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, null))

            viewModel.viewState.drop(2).test {
                dataRefreshFlow.updateStatus(InProgress(progress = 0f))
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()
                assertThat(inProgress.canCreateResource).isFalse()

                dataRefreshFlow.updateStatus(FinishedWithFailure)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()
                assertThat(finished.canCreateResource).isFalse()

                viewModel.sideEffect.test {
                    val effect = awaitItem()
                    assertIs<ShowErrorSnackbar>(effect)
                    assertThat(effect.type).isEqualTo(FAILED_TO_REFRESH_DATA)
                }
            }
        }

    @Test
    fun `should show success snackbar after resource form return`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                // create
                viewModel.onIntent(
                    HomeIntent.ResourceFormReturned(
                        resourceCreated = true,
                        resourceEdited = false,
                        resourceName = "Test Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())
                val createSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(createSnackbarEffect)
                assertEquals(RESOURCE_CREATED, createSnackbarEffect.type)
                assertEquals("Test Resource", createSnackbarEffect.message)

                // edit
                viewModel.onIntent(
                    HomeIntent.ResourceFormReturned(
                        resourceCreated = false,
                        resourceEdited = true,
                        resourceName = "Test Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())
                val editSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(editSnackbarEffect)
                assertEquals(RESOURCE_EDITED, editSnackbarEffect.type)
                assertEquals("Test Resource", editSnackbarEffect.message)
            }
        }

    @Test
    fun `should refresh data after resource details return with edit`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.ResourceDetailsReturned(
                        resourceEdited = true,
                        resourceDeleted = false,
                        resourceName = "Test Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should show success snackbar after resource details return with delete`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.ResourceDetailsReturned(
                        resourceEdited = false,
                        resourceDeleted = true,
                        resourceName = "Test Resource",
                    ),
                )

                val deleteSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(deleteSnackbarEffect)
                assertEquals(RESOURCE_DELETED, deleteSnackbarEffect.type)
                assertEquals("Test Resource", deleteSnackbarEffect.message)
                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should show success snackbar after folder created`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.FolderCreateReturned(
                        folderName = "Test Folder",
                    ),
                )

                val createSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(createSnackbarEffect)
                assertEquals(FOLDER_CREATED, createSnackbarEffect.type)
                assertEquals("Test Folder", createSnackbarEffect.message)
                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should show success snackbar after permissions updated`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.ResourceShareReturned(
                        resourceShared = true,
                    ),
                )

                val shareSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(shareSnackbarEffect)
                assertEquals(RESOURCE_SHARED, shareSnackbarEffect.type)
                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should detect autofill conflict when detector returns true on initialize`() =
        runTest {
            mockHomeData()
            val detectAutofillConflict: DetectAutofillConflict = get()
            whenever(detectAutofillConflict.invoke()).thenReturn(true)

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isTrue()
            verify(detectAutofillConflict).invoke()
        }

    @Test
    fun `should not detect autofill conflict when detector returns false on initialize`() =
        runTest {
            mockHomeData()
            val detectAutofillConflict: DetectAutofillConflict = get()
            whenever(detectAutofillConflict.invoke()).thenReturn(false)

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isFalse()
            verify(detectAutofillConflict).invoke()
        }

    @Test
    fun `initial state should have autofill conflict as false before initialize`() =
        runTest {
            viewModel = get()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isFalse()
        }

    @Test
    fun `should not regenerate homeData on refresh complete`() =
        runTest {
            mockHomeData()
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            val provider: HomeDataProvider = get()

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            clearInvocations(provider)

            dataRefreshFlow.updateStatus(InProgress(progress = 0f))
            dataRefreshFlow.updateStatus(FinishedWithSuccess)
            advanceUntilIdle()

            verify(provider, never()).provideData(any(), any(), any(), any())
        }

    @Test
    fun `should reload home data and avatar when account switches`() =
        runTest {
            mockHomeData()
            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            val newAvatar = "new_avatar_url"
            whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
                GetSelectedAccountDataUseCase.Output(
                    firstName = "New",
                    lastName = "User",
                    email = "new@passbolt.com",
                    avatarUrl = newAvatar,
                    url = "www.passbolt.com",
                    serverId = "2",
                    label = "label2",
                    role = "user",
                ),
            )

            val newHomeData = mockResourcesData()
            whenever(get<HomeDataProvider>().provideData(any(), any(), any(), any())).thenReturn(newHomeData)

            val accountSwitchFlow: AccountSwitchFlow = get()
            accountSwitchFlow.notifyAccountSwitch("id2")
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.userAvatar).isEqualTo(newAvatar)
            assertThat(viewModel.viewState.value.homeData).isEqualTo(newHomeData)
        }

    @Test
    fun `should reload home data and avatar on resume when selected account changed`() =
        runTest {
            mockHomeData()
            // first read (initialize) returns the initial account, second read (resume) a new one
            whenever(get<GetSelectedAccountUseCase>().execute(Unit)).thenReturn(
                GetSelectedAccountUseCase.Output("id1"),
                GetSelectedAccountUseCase.Output("id2"),
            )

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            val newAvatar = "new_avatar_url"
            whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
                GetSelectedAccountDataUseCase.Output(
                    firstName = "New",
                    lastName = "User",
                    email = "new@passbolt.com",
                    avatarUrl = newAvatar,
                    url = "www.passbolt.com",
                    serverId = "2",
                    label = "label2",
                    role = "user",
                ),
            )
            val newHomeData = mockResourcesData()
            whenever(get<HomeDataProvider>().provideData(any(), any(), any(), any())).thenReturn(newHomeData)

            viewModel.onIntent(OnResume)
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.userAvatar).isEqualTo(newAvatar)
            assertThat(viewModel.viewState.value.homeData).isEqualTo(newHomeData)
        }

    @Test
    fun `should not reload home data on resume when selected account unchanged`() =
        runTest {
            mockHomeData()
            whenever(get<GetSelectedAccountUseCase>().execute(Unit)).thenReturn(GetSelectedAccountUseCase.Output("id1"))
            val provider: HomeDataProvider = get()

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            clearInvocations(provider)

            viewModel.onIntent(OnResume)
            advanceUntilIdle()

            verify(provider, never()).provideData(any(), any(), any(), any())
        }

    private fun mockCanCreateResource(canCreate: Boolean) {
        get<ResourceAccessInteractor>().stub {
            onBlocking { canCreateResource(anyOrNull()) }.doReturn(canCreate)
        }
    }

    private fun mockHomeData() {
        val homeData = mockResourcesData()
        get<HomeDataProvider>().stub {
            onBlocking {
                provideData(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(homeData)
        }
    }

    private fun mockResourcesData() =
        HomeData(
            resourceList =
                flowOf(
                    PagingData.from(
                        listOf(
                            mockResourceModel("id1", "Resource 1"),
                            mockResourceModel("id2", "Resource 2"),
                        ),
                    ),
                ),
        )

    private fun mockResourceModel(
        id: String,
        name: String,
    ) = ResourceUiModel(
        resourceId = id,
        resourceTypeId = "resTypeId",
        slug = "password-and-description",
        folderId = "folderId",
        permission = ResourcePermission.READ,
        favouriteId = null,
        modified = ZonedDateTime.now(),
        expiry = null,
        metadataJsonModel =
            MetadataJsonModel(
                """
                {
                    "name": "$name",
                    "uri": "https://example.com",
                    "username": "testuser",
                    "description": "Test description"
                }
                """.trimIndent(),
            ),
        metadataKeyId = null,
        metadataKeyType = null,
    )
}
