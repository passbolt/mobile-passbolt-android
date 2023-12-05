package com.passbolt.mobile.android.folderdetails

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase.Output
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

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

class FolderDetailsPresenterTest : KoinTest {

    private val presenter: FolderDetailsContract.Presenter by inject()
    private val view: FolderDetailsContract.View = mock()

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testFolderDetailsModule)
    }

    @Before
    fun setup() {
        mockGetLocalFolderDetailsUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderDetailsUseCase.Output(MOCK_CHILD_FOLDER)
        }
        mockGetFolderLocationUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderLocationUseCase.Output(
                listOf(MOCK_PARENT_FOLDER, MOCK_CHILD_FOLDER)
            )
        }
        mockGetLocalFolderPermissionsUseCase.stub {
            onBlocking { execute(any()) } doReturn Output(
                MOCK_PERMISSIONS
            )
        }
        presenter.attach(view)
        presenter.argsRetrieved(MOCK_FOLDER_ID, 100, 100f)
    }

    @Test
    fun `view should show folder details correct`() {
        verify(view).showFolderName(MOCK_FOLDER_NAME)
        verify(view).showFolderIcon()
        verify(view, never()).showFolderSharedIcon()
    }

    @Test
    fun `view should show folder location correct`() {
        argumentCaptor<List<String>> {
            verify(view).showFolderLocation(capture())
            val expectedLocation = listOf(MOCK_PARENT_FOLDER.name, MOCK_CHILD_FOLDER.name)
            assertThat(firstValue).containsExactlyElementsIn(expectedLocation)
        }
    }

    private companion object {
        private const val MOCK_FOLDER_ID = "folderId"
        private const val MOCK_FOLDER_NAME = "folderId"
        private val MOCK_CHILD_FOLDER =
            FolderModel(MOCK_FOLDER_ID, "parentId", MOCK_FOLDER_NAME, false, ResourcePermission.UPDATE)
        private val MOCK_PARENT_FOLDER =
            FolderModel("parentId", null, "parent folder in root", false, ResourcePermission.UPDATE)
        private val MOCK_PERMISSIONS = listOf(
            PermissionModelUi.GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "groupPermId1",
                group = GroupModel(
                    groupId = "groupId1",
                    groupName = "groupname1"
                )
            ),
            PermissionModelUi.GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "groupPermId2",
                group = GroupModel(
                    groupId = "groupId2",
                    groupName = "groupname2"
                )
            )
        ) + listOf(
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.OWNER,
                permissionId = "userPermId1",
                user = UserWithAvatar(
                    userId = "userId1",
                    firstName = "first",
                    lastName = "last",
                    userName = "userName",
                    isDisabled = false,
                    avatarUrl = "avatarUrl"
                )
            ),
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.UPDATE,
                permissionId = "userPermId2",
                user = UserWithAvatar(
                    userId = "userId2",
                    firstName = "first",
                    lastName = "last",
                    userName = "userName",
                    isDisabled = false,
                    avatarUrl = "avatarUrl"
                )
            )
        )

    }

}
