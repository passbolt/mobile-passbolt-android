package com.passbolt.mobile.android.folderdetails

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
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoBack
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoToLocationDetails
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoToPermissionDetails
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.Initialize
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.SharedWithClick
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToFolderLocation
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToFolderPermissions
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToHome
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateUp
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.ShowToast
import com.passbolt.mobile.android.folderdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.folderdetails.ToastType.CONTENT_NOT_AVAILABLE
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.RbacRuleModel.DENY
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class FolderDetailsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetLocalFolderDetailsUseCase>() }
                        single { mock<GetLocalFolderLocationUseCase>() }
                        single { mock<GetLocalFolderPermissionsUseCase>() }
                        single { mock<GetRbacRulesUseCase>() }
                        singleOf(::DataRefreshTrackingFlow)
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::FolderDetailsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: FolderDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getLocalFolderDetailsUseCase = get<GetLocalFolderDetailsUseCase>()
        getLocalFolderDetailsUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderDetailsUseCase.Output(testFolder)
        }

        val getLocalFolderLocationUseCase = get<GetLocalFolderLocationUseCase>()
        getLocalFolderLocationUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderLocationUseCase.Output(testParentFolders)
        }

        val getLocalFolderPermissionsUseCase = get<GetLocalFolderPermissionsUseCase>()
        getLocalFolderPermissionsUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderPermissionsUseCase.Output(testPermissions)
        }

        val getRbacRulesUseCase = get<GetRbacRulesUseCase>()
        getRbacRulesUseCase.stub {
            onBlocking { execute(any()) } doReturn GetRbacRulesUseCase.Output(testRbacModelWithPermissions)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `folder details data should be loaded and displayed when initialized`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            viewModel.viewState.test {
                val updatedState = awaitItem()
                assertThat(updatedState.folderId).isEqualTo(testFolder.folderId)
                assertThat(updatedState.folder).isEqualTo(testFolder)
                assertThat(updatedState.locationPath).isEqualTo(testParentFolders.map { it.name })
                assertThat(updatedState.canViewPermissions).isTrue()
                assertThat(updatedState.permissions).isEqualTo(testPermissions)
                assertThat(updatedState.isRefreshing).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `folder details should not load permissions when rbac denies permission view`() =
        runTest {
            val getRbacRulesUseCase = get<GetRbacRulesUseCase>()
            getRbacRulesUseCase.stub {
                onBlocking { execute(any()) } doReturn GetRbacRulesUseCase.Output(testRbacModelWithoutPermissions)
            }

            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            viewModel.viewState.test {
                val updatedState = awaitItem()
                assertThat(updatedState.canViewPermissions).isFalse()
                assertThat(updatedState.permissions).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should emit navigate up side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertThat(awaitItem()).isEqualTo(NavigateUp)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go to location details intent should emit navigate to folder location side effect`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToLocationDetails)
                assertThat(awaitItem()).isEqualTo(NavigateToFolderLocation(testFolder.folderId))
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go to permission details intent should emit navigate to folder permissions side effect`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToPermissionDetails)
                assertThat(awaitItem()).isEqualTo(
                    NavigateToFolderPermissions(testFolder.folderId, PermissionsMode.VIEW),
                )
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `shared with click intent should emit navigate to folder permissions side effect`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            viewModel.sideEffect.test {
                viewModel.onIntent(SharedWithClick)
                assertThat(awaitItem()).isEqualTo(
                    NavigateToFolderPermissions(testFolder.folderId, PermissionsMode.VIEW),
                )
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show refreshing state during data refresh`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
            dataRefreshTrackingFlow.updateStatus(InProgress)

            viewModel.viewState.test {
                val refreshingState = awaitItem()
                assertThat(refreshingState.isRefreshing).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle data refresh failure and show error`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
            dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)

            viewModel.viewState.test {
                val updatedState = awaitItem()
                assertThat(updatedState.isRefreshing).isFalse()
            }

            viewModel.sideEffect.test {
                assertThat(awaitItem()).isEqualTo(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should reload folder details after successful data refresh`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
            dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)

            viewModel.viewState.test {
                val refreshedState = awaitItem()
                assertThat(refreshedState.isRefreshing).isFalse()
                assertThat(refreshedState.folder).isEqualTo(testFolder)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle null pointer exception when loading folder and navigate to home`() =
        runTest {
            val getLocalFolderDetailsUseCase = get<GetLocalFolderDetailsUseCase>()
            getLocalFolderDetailsUseCase.stub {
                onBlocking { execute(any()) } doThrow NullPointerException("Folder not found")
            }

            viewModel = get()
            viewModel.onIntent(Initialize(testFolder.folderId))

            viewModel.sideEffect.test {
                assertThat(awaitItem()).isEqualTo(ShowToast(CONTENT_NOT_AVAILABLE))
                assertThat(awaitItem()).isEqualTo(NavigateToHome)
            }
        }

    private companion object {
        private val testFolder by lazy {
            FolderModel(
                folderId = "folder-id-123",
                parentFolderId = "parent-folder-id",
                name = "Test Folder",
                isShared = true,
                permission = ResourcePermission.OWNER,
            )
        }

        private val testParentFolders by lazy {
            listOf(
                FolderModel(
                    folderId = "root-folder",
                    parentFolderId = null,
                    name = "Root",
                    isShared = false,
                    permission = ResourcePermission.OWNER,
                ),
                FolderModel(
                    folderId = "parent-folder-id",
                    parentFolderId = "root-folder",
                    name = "Parent Folder",
                    isShared = true,
                    permission = ResourcePermission.UPDATE,
                ),
            )
        }

        private val testPermissions by lazy {
            listOf(
                PermissionModelUi.UserPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "perm-1",
                    user =
                        UserWithAvatar(
                            userId = "user-1",
                            firstName = "Test",
                            lastName = "User",
                            userName = "test.user@example.com",
                            isDisabled = false,
                            avatarUrl = null,
                        ),
                ),
                PermissionModelUi.GroupPermissionModel(
                    permission = ResourcePermission.UPDATE,
                    permissionId = "perm-2",
                    group =
                        GroupModel(
                            groupId = "group-1",
                            groupName = "Test Group",
                        ),
                ),
            )
        }

        private val testRbacModelWithPermissions by lazy {
            RbacModel(
                passwordPreviewRule = ALLOW,
                passwordCopyRule = ALLOW,
                tagsUseRule = ALLOW,
                shareViewRule = ALLOW,
                foldersUseRule = ALLOW,
            )
        }

        private val testRbacModelWithoutPermissions by lazy {
            RbacModel(
                passwordPreviewRule = ALLOW,
                passwordCopyRule = ALLOW,
                tagsUseRule = ALLOW,
                shareViewRule = DENY,
                foldersUseRule = ALLOW,
            )
        }
    }
}
