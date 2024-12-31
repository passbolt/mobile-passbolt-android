package com.passbolt.mobile.android.permissions.permissions

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.passbolt.mobile.android.commontest.session.validSessionTestModule
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

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
// TODO add folder permissions tests
class ResourcePermissionsPresenterTest : KoinTest {

    private val presenter: PermissionsContract.Presenter by inject()
    private val view: PermissionsContract.View = mock()
    private val mockFullDataRefreshExecutor: FullDataRefreshExecutor by inject()

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testResourcePermissionsModule, validSessionTestModule)
    }

    @Before
    fun setup() {
        resourceModel = ResourceModel(
            resourceId = RESOURCE_ID,
            resourceTypeId = RESOURCE_TYPE_ID,
            folderId = FOLDER_ID_ID,
            permission = ResourcePermission.READ,
            favouriteId = null,
            modified = ZonedDateTime.now(),
            expiry = null,
            json = JsonObject().apply {
                addProperty("name", NAME)
                addProperty("username", USERNAME)
                addProperty("uri", URL)
                addProperty("description", DESCRIPTION)
            }.toString(),
            metadataKeyId = null,
            metadataKeyType = null
        )
        mockGetLocalResourceUseCase.stub {
            onBlocking { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourceUseCase.Output(resourceModel))
        }
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        presenter.attach(view)
    }

    @Test
    fun `save button and add user button should be shown in edit mode`() {
        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.EDIT)
        presenter.resume(view)

        verify(view).showSaveButton()
        verify(view).showAddUserButton()
    }

    @Test
    fun `edit button should be shown in view mode and if owner`() {
        mockGetLocalResourceUseCase.stub {
            onBlocking { execute(GetLocalResourceUseCase.Input(resourceModel.resourceId)) }
                .doReturn(GetLocalResourceUseCase.Output(resourceModel.copy(permission = ResourcePermission.OWNER)))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)

        verify(view).showEditButton()
    }

    @Test
    fun `empty state should be shown when there are no permissions`() {
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(emptyList()))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)

        verify(view).showEmptyState()
    }

    @Test
    fun `existing permissions should be shown initially`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)

        verify(view).showPermissions(mockPermissions)
    }

    @Test
    fun `view should show warning if there is not at least one owner permission`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.EDIT)
        presenter.resume(view)
        presenter.actionButtonClick()

        verify(view).showOneOwnerSnackbar()
    }

    @Test
    fun `view should not show deleted user permission`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)
        presenter.userPermissionDeleted(USER_PERMISSIONS[0])

        verify(view).showPermissions(GROUP_PERMISSIONS)
    }

    @Test
    fun `view should not show deleted group permission`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)
        presenter.groupPermissionDeleted(GROUP_PERMISSIONS[0])

        verify(view).showPermissions(USER_PERMISSIONS)
    }

    @Test
    fun `user permission modification should be reflected`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)
        val modifiedPermission = PermissionModelUi.UserPermissionModel(
            ResourcePermission.OWNER, "permId", USER_PERMISSIONS[0].user.copy()
        )
        presenter.userPermissionModified(modifiedPermission)

        argumentCaptor<List<PermissionModelUi>>().apply {
            verify(view).showPermissions(capture())
            assertThat(firstValue).contains(GROUP_PERMISSIONS[0])
            assertThat(firstValue.filterIsInstance<PermissionModelUi.UserPermissionModel>()).hasSize(1)
            assertThat(firstValue.filterIsInstance<PermissionModelUi.UserPermissionModel>()[0].permission)
                .isEqualTo(ResourcePermission.OWNER)
        }
    }

    @Test
    fun `group permission modification should be reflected`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.VIEW)
        presenter.resume(view)
        val modifiedPermission = PermissionModelUi.GroupPermissionModel(
            ResourcePermission.OWNER, "permId", GROUP_PERMISSIONS[0].group.copy()
        )
        presenter.groupPermissionModified(modifiedPermission)

        argumentCaptor<List<PermissionModelUi>>().apply {
            verify(view).showPermissions(capture())
            assertThat(firstValue).contains(USER_PERMISSIONS[0])
            assertThat(firstValue.filterIsInstance<PermissionModelUi.GroupPermissionModel>()).hasSize(1)
            assertThat(firstValue.filterIsInstance<PermissionModelUi.GroupPermissionModel>()[0].permission)
                .isEqualTo(ResourcePermission.OWNER)
        }
    }

    @Test
    fun `view should set result and go back after share success`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS[0].copy(permission = ResourcePermission.OWNER)
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }
        mockResourceShareInteractor.stub {
            onBlocking { simulateAndShareResource(any(), any()) }
                .doReturn(com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor.Output.Success)
        }
        mockHomeDataInteractor.stub {
            onBlocking { refreshAllHomeScreenData() }
                .doReturn(HomeDataInteractor.Output.Success)
        }

        presenter.argsReceived(PermissionsItem.RESOURCE, RESOURCE_ID, PermissionsMode.EDIT)
        presenter.resume(view)
        presenter.actionButtonClick()

        verify(view).showProgress()
        verifyBlocking(mockHomeDataInteractor) { refreshAllHomeScreenData() }
        verify(view).closeWithShareSuccessResult()
        verify(view).hideProgress()
    }

    private companion object {
        private const val RESOURCE_ID = "resid"
        private val GROUP_PERMISSIONS = listOf(
            PermissionModelUi.GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "groupPermId",
                group = GroupModel(groupId = "groupId", groupName = "groupname")
            )
        )
        private val USER_PERMISSIONS = listOf(
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "userPermId",
                user = UserWithAvatar(
                    userId = "userId",
                    firstName = "first",
                    lastName = "last",
                    userName = "userName",
                    isDisabled = false,
                    avatarUrl = "avartUrl"
                )
            )
        )

        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val URL = "https://www.passbolt.com"
        private const val DESCRIPTION = "desc"
        private const val RESOURCE_TYPE_ID = "resTypeId"
        private const val FOLDER_ID_ID = "folderId"
        private lateinit var resourceModel: ResourceModel
    }
}
