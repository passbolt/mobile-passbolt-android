package com.passbolt.mobile.android.permissions.permissionrecipients

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetLocalGroupsUseCase
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUsersUseCase
import com.passbolt.mobile.android.feature.resources.userspermissiondetails.mockGetLocalGroupsUseCase
import com.passbolt.mobile.android.feature.resources.userspermissiondetails.mockGetLocalUsersUseCase
import com.passbolt.mobile.android.feature.resources.userspermissiondetails.testPermissionRecipientsModule
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
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

class PermissionRecipientsPresenterTest : KoinTest {

    private val presenter: PermissionRecipientsContract.Presenter by inject()
    private val view: PermissionRecipientsContract.View = mock()
    private val permissionsModelMapper: PermissionsModelMapper by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testPermissionRecipientsModule)
    }

    @Before
    fun setup() {
        mockGetLocalUsersUseCase.stub {
            onBlocking { execute(GetLocalUsersUseCase.Input(emptyList())) }
                .doReturn(GetLocalUsersUseCase.Output(listOf(USER)))
        }
        mockGetLocalGroupsUseCase.stub {
            onBlocking { execute(GetLocalGroupsUseCase.Input(emptyList())) }
                .doReturn(GetLocalGroupsUseCase.Output(listOf(GROUP)))
        }

        presenter.attach(view)
    }

    @Test
    fun `existing permissions and recipient list should be shown`() {
        presenter.argsReceived(
            emptyList(),
            emptyList(),
            100,
            10f
        )

        verify(view).showPermissions(
            emptyList(),
            emptyList(),
            counterValue = emptyList(),
            overlap = 0
        )
        verify(view).showRecipients(listOf(GROUP), listOf(USER))
    }

    @Test
    fun `new selected group permissions should be added to permissions list`() {
        presenter.argsReceived(
            emptyList(),
            emptyList(),
            100,
            10f
        )
        reset(view)
        presenter.groupRecipientSelectionChanged(NEW_GROUP, isSelected = true)

        val groupPermissionsCaptor = argumentCaptor<List<PermissionModelUi.GroupPermissionModel>>()

        verify(view).showPermissions(
            groupPermissionsCaptor.capture(),
            any(),
            any(),
            any()
        )
        assertThat(groupPermissionsCaptor.firstValue.size).isEqualTo(1)
        assertThat(groupPermissionsCaptor.firstValue[0].group).isEqualTo(NEW_GROUP)
        assertThat(groupPermissionsCaptor.firstValue[0].permission)
            .isEqualTo(PermissionRecipientsPresenter.DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS)
    }

    @Test
    fun `new selected user permissions should be added to permissions list`() {
        presenter.argsReceived(
            emptyList(),
            emptyList(),
            100,
            10f
        )
        reset(view)
        presenter.userRecipientSelectionChanged(NEW_USER, isSelected = true)

        val userPermissionsCaptor = argumentCaptor<List<PermissionModelUi.UserPermissionModel>>()
        val mapped = permissionsModelMapper.map(
            NEW_USER, PermissionRecipientsPresenter.DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS, "permId"
        )

        verify(view).showPermissions(
            any(),
            userPermissionsCaptor.capture(),
            any(),
            any()
        )
        assertThat(userPermissionsCaptor.firstValue.size).isEqualTo(1)
        assertThat(userPermissionsCaptor.firstValue[0].user.userId).isEqualTo(mapped.user.userId)
        assertThat(userPermissionsCaptor.firstValue[0].permission)
            .isEqualTo(PermissionRecipientsPresenter.DEFAULT_PERMISSIONS_FOR_NEW_RECIPIENTS)
    }

    @Test
    fun `filter icon should be updated correctly`() {
        presenter.argsReceived(
            emptyList(),
            emptyList(),
            100,
            10f
        )

        presenter.searchTextChange("abc")
        presenter.searchTextChange("")

        verify(view).showClearSearchIcon()
        verify(view).hideClearSearchIcon()
    }

    private companion object {
        private val GROUP = GroupModel("grId", "grName")
        private val NEW_GROUP = GroupModel("grId", "grName")

        private val USER_WITH_AVATAR = UserWithAvatar(
            "userId", "first", "last", "userName", "avartUrl"
        )
        private val USER = UserModel(
            USER_WITH_AVATAR.userId,
            USER_WITH_AVATAR.userName,
            GpgKeyModel("keyData", "fingerprint", 1, "uid", "keyid", "rsa", ZonedDateTime.now(), ZonedDateTime.now()),
            UserProfileModel(
                "username",
                USER_WITH_AVATAR.firstName,
                USER_WITH_AVATAR.lastName,
                USER_WITH_AVATAR.avatarUrl
            )
        )
        private val NEW_USER = UserModel(
            "newUserId",
            "newUserName",
            GpgKeyModel("keyData", "fingerprint", 1, "uid", "keyid", "rsa", ZonedDateTime.now(), ZonedDateTime.now()),
            UserProfileModel("username", "newUserFirst", "newUserLast", "newUserAvatar")
        )
    }
}
