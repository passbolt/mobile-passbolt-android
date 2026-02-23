package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.CancelPermissionDelete
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.ConfirmPermissionDelete
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.DeletePermission
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.Save
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.SeeGroupMembers
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.SelectPermission
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateToGroupMembers
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetDeletePermissionResult
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetUpdatedPermissionResult
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.GroupWithUsersModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.PermissionsMode.EDIT
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourcePermission.UPDATE
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
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
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class GroupPermissionsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetGroupWithUsersUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factory { (permission: PermissionModelUi.GroupPermissionModel, mode: PermissionsMode) ->
                            GroupPermissionsViewModel(
                                mode = mode,
                                permission = permission,
                                getGroupWithUsersUseCase = get(),
                                coroutineLaunchContext = get(),
                            )
                        }
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: GroupPermissionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getGroupWithUsersUseCase = get<GetGroupWithUsersUseCase>()
        getGroupWithUsersUseCase.stub {
            onBlocking { execute(GetGroupWithUsersUseCase.Input(GROUP.groupId)) }
                .doReturn(GetGroupWithUsersUseCase.Output(GROUP_WITH_USERS))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize in view mode should set state correctly`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, PermissionsMode.VIEW) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.groupPermission).isEqualTo(GROUP_PERMISSION)
                assertThat(state.isEditMode).isFalse()
            }
        }

    @Test
    fun `initialize in edit mode should set state correctly`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.groupPermission).isEqualTo(GROUP_PERMISSION)
                assertThat(state.isEditMode).isTrue()
            }
        }

    @Test
    fun `selecting permission should update group permission in state`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SelectPermission(UPDATE))
                assertThat(awaitItem().groupPermission.permission).isEqualTo(UPDATE)
            }
        }

    @Test
    fun `save should emit updated permission result and navigate back`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.onIntent(SelectPermission(UPDATE))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val setResult = awaitItem()
                assertIs<SetUpdatedPermissionResult>(setResult)
                assertThat(setResult.permission.permission).isEqualTo(UPDATE)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `delete permission should show confirmation then emit result and navigate back`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DeletePermission)
                assertThat(awaitItem().isDeleteConfirmationVisible).isTrue()
            }

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmPermissionDelete)
                assertThat(viewModel.viewState.value.isDeleteConfirmationVisible).isFalse()

                val deleteResult = awaitItem()
                assertIs<SetDeletePermissionResult>(deleteResult)
                assertThat(deleteResult.permission).isEqualTo(GROUP_PERMISSION)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `cancel delete should hide confirmation dialog`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DeletePermission)
                assertThat(awaitItem().isDeleteConfirmationVisible).isTrue()

                viewModel.onIntent(CancelPermissionDelete)
                assertThat(awaitItem().isDeleteConfirmationVisible).isFalse()
            }
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, PermissionsMode.VIEW) }

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `see group members should emit navigate to group members side effect`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, PermissionsMode.VIEW) }

            viewModel.sideEffect.test {
                viewModel.onIntent(SeeGroupMembers)

                val effect = awaitItem()
                assertIs<NavigateToGroupMembers>(effect)
                assertThat(effect.groupId).isEqualTo(GROUP.groupId)
            }
        }

    private companion object {
        private val USER =
            UserModel(
                id = "userId",
                userName = "userName",
                disabled = false,
                gpgKey =
                    GpgKeyModel(
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
                    UserProfileModel(
                        username = "username",
                        firstName = "first",
                        lastName = "last",
                        avatarUrl = "avatarUrl",
                    ),
            )
        private val GROUP = GroupModel("grId", "grName")
        private val GROUP_PERMISSION = PermissionModelUi.GroupPermissionModel(ResourcePermission.READ, "permId", GROUP)
        private val GROUP_WITH_USERS = GroupWithUsersModel(GROUP, listOf(USER))
    }
}
