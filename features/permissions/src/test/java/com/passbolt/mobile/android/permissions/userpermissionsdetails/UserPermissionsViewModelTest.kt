package com.passbolt.mobile.android.permissions.userpermissionsdetails

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.CancelPermissionDelete
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.ConfirmPermissionDelete
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.DeletePermission
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.Save
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.SelectPermission
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetDeletePermissionResult
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetUpdatedPermissionResult
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.PermissionsMode.EDIT
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourcePermission.UPDATE
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
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
class UserPermissionsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetLocalUserUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::UserPermissionsViewModel)
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: UserPermissionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getLocalUserUseCase = get<GetLocalUserUseCase>()
        getLocalUserUseCase.stub {
            onBlocking { execute(GetLocalUserUseCase.Input(USER_WITH_AVATAR.userId)) }
                .doReturn(GetLocalUserUseCase.Output(USER))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize in view mode should set state correctly`() =
        runTest {
            viewModel = get(parameters = { parametersOf(PermissionsMode.VIEW, USER_PERMISSION) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permission).isEqualTo(USER_PERMISSION)
                assertThat(state.isEditMode).isFalse()
            }
        }

    @Test
    fun `initialize in edit mode should set state correctly`() =
        runTest {
            viewModel = get(parameters = { parametersOf(EDIT, USER_PERMISSION) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permission).isEqualTo(USER_PERMISSION)
                assertThat(state.isEditMode).isTrue()
            }
        }

    @Test
    fun `selecting permission should update user permission in state`() =
        runTest {
            viewModel = get(parameters = { parametersOf(EDIT, USER_PERMISSION) })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SelectPermission(UPDATE))
                assertThat(awaitItem().permission.permission).isEqualTo(UPDATE)
            }
        }

    @Test
    fun `save should emit updated permission result`() =
        runTest {
            viewModel = get(parameters = { parametersOf(EDIT, USER_PERMISSION) })
            viewModel.onIntent(SelectPermission(UPDATE))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val setResult = awaitItem()
                assertIs<SetUpdatedPermissionResult>(setResult)
                assertThat(setResult.permission.permission).isEqualTo(UPDATE)
            }
        }

    @Test
    fun `delete permission should show confirmation then emit result`() =
        runTest {
            viewModel = get(parameters = { parametersOf(EDIT, USER_PERMISSION) })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DeletePermission)
                assertThat(awaitItem().isDeleteConfirmationVisible).isTrue()
            }

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmPermissionDelete)
                assertThat(viewModel.viewState.value.isDeleteConfirmationVisible).isFalse()

                val deleteResult = awaitItem()
                assertIs<SetDeletePermissionResult>(deleteResult)
                assertThat(deleteResult.permission).isEqualTo(USER_PERMISSION)
            }
        }

    @Test
    fun `cancel delete should hide confirmation dialog`() =
        runTest {
            viewModel = get(parameters = { parametersOf(EDIT, USER_PERMISSION) })

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
            viewModel = get(parameters = { parametersOf(PermissionsMode.VIEW, USER_PERMISSION) })

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    private companion object {
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
            UserModel(
                id = USER_WITH_AVATAR.userId,
                userName = USER_WITH_AVATAR.userName,
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
                        firstName = USER_WITH_AVATAR.firstName,
                        lastName = USER_WITH_AVATAR.lastName,
                        avatarUrl = USER_WITH_AVATAR.avatarUrl,
                    ),
            )
        private val USER_PERMISSION =
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "permId",
                user = USER_WITH_AVATAR,
            )
    }
}
