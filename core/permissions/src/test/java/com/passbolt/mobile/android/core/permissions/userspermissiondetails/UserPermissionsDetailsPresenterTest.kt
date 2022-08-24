package com.passbolt.mobile.android.core.permissions.userspermissiondetails

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.impl.users.GetLocalUserUseCase
import com.passbolt.mobile.android.core.permissions.permissions.ResourcePermissionsMode
import com.passbolt.mobile.android.core.permissions.userpermissionsdetails.UserPermissionsContract
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
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

class UserPermissionsDetailsPresenterTest : KoinTest {

    private val presenter: UserPermissionsContract.Presenter by inject()
    private val view: UserPermissionsContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testUserPermissionsDetailsModule)
    }

    @Before
    fun setup() {
        presenter.attach(view)
        mockGetLocalUserUseCase.stub {
            onBlocking { execute(GetLocalUserUseCase.Input(USER_WITH_AVATAR.userId)) }
                .doReturn(GetLocalUserUseCase.Output(USER))
        }
    }

    @Test
    fun `read only permission should be shown in read mode`() {
        presenter.argsRetrieved(
            USER_PERMISSION,
            ResourcePermissionsMode.VIEW
        )

        verify(view).showPermission(USER_PERMISSION.permission)
    }

    @Test
    fun `editable permission should be shown in edit mode`() {
        presenter.argsRetrieved(
            USER_PERMISSION,
            ResourcePermissionsMode.EDIT
        )

        verify(view).showPermissionChoices(USER_PERMISSION.permission)
        verify(view).showSaveLayout()
    }

    @Test
    fun `user details should be shown`() {
        presenter.argsRetrieved(
            USER_PERMISSION,
            ResourcePermissionsMode.EDIT
        )

        verify(view).showUserData(USER)
    }

    @Test
    fun `permission update should be handled correct`() {
        presenter.argsRetrieved(
            USER_PERMISSION,
            ResourcePermissionsMode.EDIT
        )
        presenter.onPermissionSelected(ResourcePermission.UPDATE)
        presenter.saveClick()

        argumentCaptor<PermissionModelUi.UserPermissionModel> {
            verify(view).setUpdatedPermissionResult(capture())
            assertThat(firstValue.permission).isEqualTo(ResourcePermission.UPDATE)
        }
        verify(view).navigateBack()
    }

    @Test
    fun `permission deletion should be handled correct`() {
        presenter.argsRetrieved(
            USER_PERMISSION,
            ResourcePermissionsMode.EDIT
        )
        presenter.deletePermissionClick()
        presenter.permissionDeleteConfirmClick()

        verify(view).showPermissionDeleteConfirmation()
        verify(view).setDeletePermissionResult(USER_PERMISSION)
        verify(view).navigateBack()
    }

    private companion object {
        private val USER_WITH_AVATAR = UserWithAvatar(
            "userId", "first", "last", "userName", "avartUrl"
        )
        private val USER = UserModel(
            USER_WITH_AVATAR.userId,
            USER_WITH_AVATAR.userName,
            GpgKeyModel("keyData", "fingerprint", 1, "uid", "keyid", "rsa", ZonedDateTime.now()),
            UserProfileModel(USER_WITH_AVATAR.firstName, USER_WITH_AVATAR.lastName, USER_WITH_AVATAR.avatarUrl)
        )
        private val USER_PERMISSION =
            PermissionModelUi.UserPermissionModel(ResourcePermission.READ, "permId", USER_WITH_AVATAR)
    }
}
