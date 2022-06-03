package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.request.SharePermission
import com.passbolt.mobile.android.ui.PermissionModelUi

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

class SharePermissionsModelMapper(
    private val permissionsModelMapper: PermissionsModelMapper
) {
    /**
     * Maps user chosen share recipients to simulate share request input.
     * During share simulation it is required to pass only new permissions and deleted ones (and not updated).
     *
     * @param resourceId Id of the shared resource
     * @param recipients User selected share recipients
     * @param existingResourcePermissions Permissions that are already applied to the resource
     */
    fun mapForSimulation(
        resourceId: String,
        recipients: List<PermissionModelUi>,
        existingResourcePermissions: List<PermissionModelUi>
    ): List<SharePermission> =
        extractNewPermissions(resourceId, recipients) +
                extractDeletedPermissions(resourceId, existingResourcePermissions, recipients)

    /**
     * Maps user chosen share recipients to share request input.
     * During share it is required to pass new, updated and deleted permissions.
     *
     * @param resourceId Id of the shared resource
     * @param recipients User selected share recipients
     * @param existingResourcePermissions Permissions that are already applied to the resource
     */
    fun mapForShare(
        resourceId: String,
        recipients: List<PermissionModelUi>,
        existingResourcePermissions: List<PermissionModelUi>
    ): List<SharePermission> =
        extractUpdatedPermissions(resourceId, existingResourcePermissions, recipients) +
                extractNewPermissions(resourceId, recipients) +
                extractDeletedPermissions(resourceId, existingResourcePermissions, recipients)

    /**
     * Extracts permissions that had been already added to the resource but were updated by the user during sharing.
     * Updated permissions are in both existing resource permissions and chosen recipients but with different
     * ResourcePermission value.
     *
     * @param existingResourcePermissions Permissions that are already applied to the resource
     * @param resourceId Id of the shared resource
     * @param recipients User selected share recipients
     */
    private fun extractUpdatedPermissions(
        resourceId: String,
        existingResourcePermissions: List<PermissionModelUi>,
        recipients: List<PermissionModelUi>
    ) = recipients
        .filter { filteredPermission ->
            existingResourcePermissions
                .map { it.permissionId }.contains(filteredPermission.permissionId) &&
                    existingResourcePermissions
                        .first { it.permissionId == filteredPermission.permissionId }
                        .permission != filteredPermission.permission
        }
        .map { permission ->
            val (aro, aroId) = prepareAroWithIdFromPermission(permission)
            SharePermission.UpdatedSharePermission(
                aro = aro,
                aroForeignKey = aroId,
                aco = ACO_RESOURCE,
                acoForeignKey = resourceId,
                type = permissionsModelMapper.mapToPermissionInt(permission.permission),
                id = permission.permissionId
            )
        }

    /**
     * Extracts permissions that had been already added to the resource but were deleted by the user during sharing.
     * Deleted permissions are present in the existing resource permissions and not present in chosen recipients.
     *
     * @param existingResourcePermissions Permissions that are already applied to the resource
     * @param resourceId Id of the shared resource
     * @param recipients User selected share recipients
     */
    private fun extractDeletedPermissions(
        resourceId: String,
        existingResourcePermissions: List<PermissionModelUi>,
        recipients: List<PermissionModelUi>
    ) = existingResourcePermissions
        .filter { existingPermission ->
            !recipients
                .map { recipient -> recipient.permissionId }
                .contains(existingPermission.permissionId)
        }
        .map { permission ->
            val (aro, aroId) = prepareAroWithIdFromPermission(permission)
            SharePermission.DeletedSharePermission(
                id = permission.permissionId,
                aro = aro,
                aroForeignKey = aroId,
                aco = ACO_RESOURCE,
                acoForeignKey = resourceId,
                type = permissionsModelMapper.mapToPermissionInt(permission.permission)
            )
        }

    /**
     * Extracts new permissions from recipients list.
     * New permission do not have IDs yet - can be filter by assigned common, constant id.
     *
     * @param recipients Recipients list
     * @param resourceId Id of the resource
     */
    private fun extractNewPermissions(
        resourceId: String,
        recipients: List<PermissionModelUi>
    ) = recipients
        .filter { it.permissionId == TEMPORARY_NEW_PERMISSION_ID }
        .map { permission ->
            val (aro, aroId) = prepareAroWithIdFromPermission(permission)
            SharePermission.NewSharePermission(
                aro = aro,
                aroForeignKey = aroId,
                aco = ACO_RESOURCE,
                acoForeignKey = resourceId,
                type = permissionsModelMapper.mapToPermissionInt(permission.permission)
            )
        }

    private fun prepareAroWithIdFromPermission(permission: PermissionModelUi) =
        when (permission) {
            is PermissionModelUi.GroupPermissionModel -> ARO_GROUP to permission.group.groupId
            is PermissionModelUi.UserPermissionModel -> ARO_USER to permission.user.userId
        }

    companion object {
        const val TEMPORARY_NEW_PERMISSION_ID = "TEMPORARY_PERMISSION_ID"

        private const val ARO_GROUP = "Group"
        private const val ARO_USER = "User"
        private const val ACO_RESOURCE = "Resource"
    }
}
