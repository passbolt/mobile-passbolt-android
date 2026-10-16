package com.passbolt.mobile.android.permissions.permissionrecipients

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode
import com.passbolt.mobile.android.domain.groups.usecase.GetLocalGroupsUseCase
import com.passbolt.mobile.android.domain.users.usecase.GetLocalUsersUseCase
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.GoBack
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.Save
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.Search
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleGroupSelection
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleUserSelection
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBackWithResult
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsViewModel.Companion.SEARCH_DEBOUNCE
import com.passbolt.mobile.android.ui.GpgKeyUiModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserProfileUiModel
import com.passbolt.mobile.android.ui.UserUiModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionRecipientsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetLocalGroupsUseCase>() }
                    single { mock<GetLocalUsersUseCase>() }
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    factory { GroupsModelMapper() }
                    factory { UsersModelMapper() }
                    factory { PermissionsModelMapper(groupsModelMapper = get(), usersModelMapper = get()) }
                    singleOf(::SessionRefreshTrackingFlow)
                    factory { params ->
                        PermissionRecipientsViewModel(
                            alreadyAddedGroupPermissions = params.get(),
                            alreadyAddedUserPermissions = params.get(),
                            getLocalGroupsUseCase = get(),
                            getLocalUsersUseCase = get(),
                            permissionsModelMapper = get(),
                            coroutineLaunchContext = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PermissionRecipientsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        get<GetLocalGroupsUseCase>().stub {
            on { execute(GetLocalGroupsUseCase.Input(searchQuery = "")) }
                .doReturn(GetLocalGroupsUseCase.Output(listOf(GROUP, EXISTING_GROUP)))
            on { execute(GetLocalGroupsUseCase.Input(searchQuery = GROUP.groupName)) }
                .doReturn(GetLocalGroupsUseCase.Output(listOf(GROUP)))
            on { execute(GetLocalGroupsUseCase.Input(searchQuery = EXISTING_GROUP.groupName)) }
                .doReturn(GetLocalGroupsUseCase.Output(listOf(EXISTING_GROUP)))
            on { execute(GetLocalGroupsUseCase.Input(searchQuery = NON_MATCHING_QUERY)) }
                .doReturn(GetLocalGroupsUseCase.Output(emptyList()))
        }
        get<GetLocalUsersUseCase>().stub {
            on { execute(GetLocalUsersUseCase.Input(searchQuery = "")) }
                .doReturn(GetLocalUsersUseCase.Output(listOf(USER)))
            on { execute(GetLocalUsersUseCase.Input(searchQuery = GROUP.groupName)) }
                .doReturn(GetLocalUsersUseCase.Output(emptyList()))
            on { execute(GetLocalUsersUseCase.Input(searchQuery = EXISTING_GROUP.groupName)) }
                .doReturn(GetLocalUsersUseCase.Output(emptyList()))
            on { execute(GetLocalUsersUseCase.Input(searchQuery = NON_MATCHING_QUERY)) }
                .doReturn(GetLocalUsersUseCase.Output(emptyList()))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load recipients excluding already added`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(arrayOf(EXISTING_GROUP_PERMISSION), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.filteredGroups).containsExactly(GROUP)
                assertThat(state.filteredUsers).containsExactly(USER)
                assertThat(state.filteredExistingPermissions).isEmpty()
                assertThat(state.currentPermissions).containsExactly(EXISTING_GROUP_PERMISSION)
            }
        }

    @Test
    fun `search should filter groups and users and update icon mode`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search(GROUP.groupName))

                val typedState = awaitItem()
                assertThat(typedState.searchInputEndIconMode).isEqualTo(SearchInputEndIconMode.CLEAR)

                advanceTimeBy(SEARCH_DEBOUNCE + 1.milliseconds)

                val searchedState = awaitItem()
                assertThat(searchedState.filteredGroups).containsExactly(GROUP)
                assertThat(searchedState.filteredUsers).isEmpty()
                assertThat(searchedState.showEmptyState).isFalse()
            }
        }

    @Test
    fun `search should apply only the latest query when typing multiple characters quickly`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.onIntent(Search("g"))
            viewModel.onIntent(Search("gr"))
            viewModel.onIntent(Search(GROUP.groupName))
            advanceUntilIdle()

            verify(get<GetLocalGroupsUseCase>(), never()).execute(GetLocalGroupsUseCase.Input(searchQuery = "g"))
            verify(get<GetLocalGroupsUseCase>(), never()).execute(GetLocalGroupsUseCase.Input(searchQuery = "gr"))
            verify(get<GetLocalGroupsUseCase>()).execute(GetLocalGroupsUseCase.Input(searchQuery = GROUP.groupName))
            verify(get<GetLocalUsersUseCase>()).execute(GetLocalUsersUseCase.Input(searchQuery = GROUP.groupName))
        }

    @Test
    fun `empty search should reset icon mode to NONE`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search(NON_MATCHING_QUERY))
                awaitItem()
                advanceTimeBy(SEARCH_DEBOUNCE + 1.milliseconds)
                awaitItem()

                viewModel.onIntent(Search(""))
                val state = awaitItem()
                assertThat(state.searchInputEndIconMode).isEqualTo(SearchInputEndIconMode.NONE)

                advanceTimeBy(SEARCH_DEBOUNCE + 1.milliseconds)
                val resetState = awaitItem()
                assertThat(resetState.filteredGroups).containsExactly(GROUP, EXISTING_GROUP)
                assertThat(resetState.filteredUsers).containsExactly(USER)
                assertThat(resetState.filteredExistingPermissions).isEmpty()
                assertThat(resetState.showEmptyState).isFalse()
            }
        }

    @Test
    fun `search with no results should show empty state`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search(NON_MATCHING_QUERY))
                awaitItem()
                advanceTimeBy(SEARCH_DEBOUNCE + 1.milliseconds)

                val state = awaitItem()
                assertThat(state.showEmptyState).isTrue()
            }
        }

    @Test
    fun `toggle group selection should update selected groups and display permissions`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleGroupSelection(GROUP))
                val state = awaitItem()
                assertThat(state.selectedGroups).containsExactly(GROUP.groupId, GROUP)
                val recomputed = awaitItem()
                assertThat(recomputed.currentPermissions).hasSize(1)
            }
        }

    @Test
    fun `toggle group selection twice should deselect`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleGroupSelection(GROUP))
                skipItems(1) // recomputed
                awaitItem()

                viewModel.onIntent(ToggleGroupSelection(GROUP))
                val state = awaitItem()
                assertThat(state.selectedGroups).isEmpty()
                val recomputed = awaitItem()
                assertThat(recomputed.currentPermissions).isEmpty()
            }
        }

    @Test
    fun `toggle user selection should update selected users and display permissions`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleUserSelection(USER))
                val state = awaitItem()
                assertThat(state.selectedUsers).containsExactly(USER.id, USER)
                val recomputed = awaitItem()
                assertThat(recomputed.currentPermissions).hasSize(1)
            }
        }

    @Test
    fun `selection should survive a search that hides the selected recipient`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.onIntent(ToggleUserSelection(USER))
            viewModel.onIntent(Search(GROUP.groupName))
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)
                val effect = awaitItem()
                assertIs<NavigateBackWithResult>(effect)
                assertThat(effect.permissions).hasSize(1)
                assertIs<UserPermissionModel>(effect.permissions[0])
            }
        }

    @Test
    fun `save should emit navigate back with result`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.onIntent(ToggleGroupSelection(GROUP))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)
                val effect = awaitItem()
                assertIs<NavigateBackWithResult>(effect)
                assertThat(effect.permissions).hasSize(1)
                assertThat(effect.permissions[0].permission).isEqualTo(ResourcePermission.READ)
            }
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `search should show existing permissions that match`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(arrayOf(EXISTING_GROUP_PERMISSION), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search(EXISTING_GROUP.groupName))
                awaitItem()
                advanceTimeBy(SEARCH_DEBOUNCE + 1.milliseconds)

                val state = awaitItem()
                assertThat(state.filteredExistingPermissions).containsExactly(EXISTING_GROUP_PERMISSION)
                assertThat(state.filteredGroups).isEmpty()
                assertThat(state.showEmptyState).isFalse()
            }
        }

    private companion object {
        private const val NON_MATCHING_QUERY = "nonexistent"

        private val GROUP = GroupModel(groupId = "grId", groupName = "grName")
        private val EXISTING_GROUP = GroupModel(groupId = "existingGrId", groupName = "existingGroup")
        private val EXISTING_GROUP_PERMISSION =
            GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "existingPermId",
                group = EXISTING_GROUP,
            )

        private val USER_WITH_AVATAR =
            UserWithAvatar(
                userId = "userId",
                firstName = "first",
                lastName = "last",
                userName = "userName",
                isDisabled = false,
                avatarUrl = "avatarUrl",
            )
        private val USER =
            UserUiModel(
                id = USER_WITH_AVATAR.userId,
                userName = USER_WITH_AVATAR.userName,
                disabled = false,
                gpgKey =
                    GpgKeyUiModel(
                        armoredKey = "keyData",
                        fingerprint = "fingerprint",
                        bits = 1,
                        uid = "uid",
                        keyId = "keyid",
                        type = "rsa",
                        keyExpirationDate = ZonedDateTime.now(),
                        keyCreationDate = ZonedDateTime.now(),
                        id = UUID.randomUUID().toString(),
                    ),
                profile =
                    UserProfileUiModel(
                        username = "username",
                        firstName = USER_WITH_AVATAR.firstName,
                        lastName = USER_WITH_AVATAR.lastName,
                        avatarUrl = USER_WITH_AVATAR.avatarUrl,
                    ),
            )
    }
}
