package com.passbolt.mobile.android.feature.resources.permissions

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.data.interactor.HomeDataInteractor
import com.passbolt.mobile.android.data.interactor.ShareInteractor
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
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

class ResourcePermissionsPresenterTest : KoinTest {

    private val presenter: ResourcePermissionsContract.Presenter by inject()
    private val view: ResourcePermissionsContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testResourcePermissionsModule)
    }

    @Before
    fun setup() {
        presenter.attach(view)
    }

    @Test
    fun `save button and add user button should be showed in edit mode`() {
        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.EDIT)

        verify(view).showSaveButton()
        verify(view).showAddUserButton()
    }

    @Test
    fun `empty state should be shown when there are no permissions`() {
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(emptyList()))
        }

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)

        verify(view).showEmptyState()
    }

    @Test
    fun `existing permissions should be shown initially`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)

        verify(view).showPermissions(mockPermissions)
    }

    @Test
    fun `view should show warning if there is not at least one owner permission`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)
        presenter.saveClick()

        verify(view).showOneOwnerSnackbar()
    }

    @Test
    fun `view should not show deleted user permission`() {
        val mockPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(mockPermissions))
        }

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)
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

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)
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

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)
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

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)
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
        mockShareInteractor.stub {
            onBlocking { simulateAndShare(any(), any()) }
                .doReturn(ShareInteractor.Output.Success)
        }
        mockHomeDataInteractor.stub {
            onBlocking { refreshAllHomeScreenData() }
                .doReturn(HomeDataInteractor.Output.Success)
        }

        presenter.argsReceived(RESOURCE_ID, ResourcePermissionsMode.VIEW)
        presenter.saveClick()

        verify(view).showProgress()
        verifyBlocking(mockHomeDataInteractor) { refreshAllHomeScreenData() }
        verify(view).closeWithShareSuccessResult()
        verify(view).hideProgress()
    }

    private companion object {
        private const val RESOURCE_ID = "resid"
        private val GROUP_PERMISSIONS = listOf(
            PermissionModelUi.GroupPermissionModel(
                ResourcePermission.READ,
                "groupPermId",
                GroupModel("groupId", "groupname")
            )
        )
        private val USER_PERMISSIONS = listOf(
            PermissionModelUi.UserPermissionModel(
                ResourcePermission.READ,
                "userPermId",
                UserWithAvatar("userId", "first", "last", "userName", "avartUrl")
            )
        )
    }
}