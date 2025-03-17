package com.passbolt.mobile.android.mappers

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.dto.request.SharePermission
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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
class SharePermissionsModelMapperTest : KoinTest {

    private val mapper: SharePermissionsModelMapper by inject()
    private val permissionModelMapper: PermissionsModelMapper by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testMappersModule)
    }

    @Test
    fun `share and simulate permissions should be mapped correct for not changed recipients`() {
        val recipients = EXISTING_PERMISSIONS

        val resultForShare = mapper.mapForShare(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )
        val resultForSimulation = mapper.mapForSimulation(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )

        assertThat(resultForShare).isEmpty()
        assertThat(resultForSimulation).isEmpty()
    }

    @Test
    fun `share and simulate permissions should be mapped correct for deleted recipient`() {
        val recipients = EXISTING_PERMISSIONS - EXISTING_PERMISSIONS[0]

        val resultForShare = mapper.mapForShare(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )
        val resultForSimulation = mapper.mapForSimulation(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )

        assertThat(resultForShare).hasSize(1)
        assertThat(resultForShare[0]).isInstanceOf(SharePermission.DeletedSharePermission::class.java)
        assertThat((resultForShare[0] as SharePermission.DeletedSharePermission).id)
            .isEqualTo(EXISTING_PERMISSIONS[0].permissionId)

        assertThat(resultForSimulation).hasSize(1)
        assertThat(resultForSimulation[0]).isInstanceOf(SharePermission.DeletedSharePermission::class.java)
        assertThat((resultForSimulation[0] as SharePermission.DeletedSharePermission).id)
            .isEqualTo(EXISTING_PERMISSIONS[0].permissionId)
    }

    @Test
    fun `share and simulate permissions should be mapped correct for new recipient`() {
        val newRecipient = PermissionModelUi.GroupPermissionModel(
            ResourcePermission.UPDATE,
            SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID,
            GroupModel("newGrId", "newGrName")
        )
        val recipients = EXISTING_PERMISSIONS + newRecipient

        val resultForShare = mapper.mapForShare(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )
        val resultForSimulation = mapper.mapForSimulation(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )

        assertThat(resultForShare).hasSize(1)
        assertThat(resultForShare[0]).isInstanceOf(SharePermission.NewSharePermission::class.java)
        assertThat((resultForShare[0] as SharePermission.NewSharePermission).aroForeignKey)
            .isEqualTo(newRecipient.group.groupId)

        assertThat(resultForSimulation).hasSize(1)
        assertThat(resultForSimulation[0]).isInstanceOf(SharePermission.NewSharePermission::class.java)
        assertThat((resultForSimulation[0] as SharePermission.NewSharePermission).aroForeignKey)
            .isEqualTo(newRecipient.group.groupId)
    }

    @Test
    fun `share and simulate permissions should be mapped correct for updated recipient`() {
        val recipients = List(EXISTING_PERMISSIONS.size) {
            if (it == 0) {
                val firstMockPermission =
                    EXISTING_PERMISSIONS[0] as PermissionModelUi.GroupPermissionModel
                PermissionModelUi.GroupPermissionModel(
                    ResourcePermission.OWNER,
                    firstMockPermission.permissionId,
                    firstMockPermission.group.copy()
                )
            } else {
                EXISTING_PERMISSIONS[it]
            }
        }

        val resultForShare = mapper.mapForShare(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )
        val resultForSimulation = mapper.mapForSimulation(
            SharePermissionsModelMapper.ShareItem.Resource(RESOURCE_ID),
            recipients,
            EXISTING_PERMISSIONS
        )

        assertThat(resultForShare).hasSize(1)
        assertThat(resultForShare[0]).isInstanceOf(SharePermission.UpdatedSharePermission::class.java)
        assertThat((resultForShare[0] as SharePermission.UpdatedSharePermission).aroForeignKey)
            .isEqualTo((EXISTING_PERMISSIONS[0] as PermissionModelUi.GroupPermissionModel).group.groupId)
        assertThat((resultForShare[0] as SharePermission.UpdatedSharePermission).type)
            .isEqualTo(permissionModelMapper.mapToPermissionInt(ResourcePermission.OWNER))

        assertThat(resultForSimulation).isEmpty()
    }

    private companion object {
        private const val RESOURCE_ID = "resId"
        private val EXISTING_PERMISSIONS = listOf(
            PermissionModelUi.GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "groupPermId1",
                group = GroupModel(groupId = "groupId1", groupName = "groupname1")
            ),
            PermissionModelUi.GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "groupPermId2",
                group = GroupModel(groupId = "groupId2", groupName = "groupname2")
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
