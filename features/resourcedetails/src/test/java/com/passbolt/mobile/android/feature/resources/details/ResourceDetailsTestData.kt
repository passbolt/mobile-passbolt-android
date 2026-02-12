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

package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.TagModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import java.time.ZonedDateTime
import java.util.UUID

internal const val NAME = "Test Resource"
internal const val USERNAME = "john.doe@example.com"
internal const val URL = "https://www.passbolt.com"
internal const val DESCRIPTION = "Test description"
internal val ID: String = UUID.randomUUID().toString()
internal val RESOURCE_TYPE_ID: UUID = UUID.randomUUID()
internal const val FOLDER_ID = "folderId"

internal val DEFAULT_RESOURCE_MODEL by lazy {
    ResourceModel(
        resourceId = ID,
        resourceTypeId = RESOURCE_TYPE_ID.toString(),
        folderId = FOLDER_ID,
        permission = ResourcePermission.OWNER,
        favouriteId = null,
        modified = ZonedDateTime.now(),
        expiry = null,
        metadataJsonModel =
            MetadataJsonModel(
                """
                {
                    "name": "$NAME",
                    "uri": "$URL",
                    "username": "$USERNAME",
                    "description": "$DESCRIPTION"
                }
                """.trimIndent(),
            ),
        metadataKeyId = null,
        metadataKeyType = null,
    )
}

internal val DEFAULT_FEATURE_FLAGS =
    FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = true,
        areFoldersAvailable = true,
        areTagsAvailable = true,
        isTotpAvailable = true,
        isRbacAvailable = true,
        isPasswordExpiryAvailable = true,
        arePasswordPoliciesAvailable = true,
        canUpdatePasswordPolicies = true,
        isV5MetadataAvailable = false,
    )

internal val DEFAULT_RBAC =
    RbacModel(
        passwordPreviewRule = ALLOW,
        passwordCopyRule = ALLOW,
        tagsUseRule = ALLOW,
        shareViewRule = ALLOW,
        foldersUseRule = ALLOW,
    )

internal val GROUP_PERMISSION =
    PermissionModelUi.GroupPermissionModel(
        permission = ResourcePermission.READ,
        permissionId = "permId1",
        group = GroupModel(groupId = "grId", groupName = "grName"),
    )

internal val USER_PERMISSION =
    PermissionModelUi.UserPermissionModel(
        permission = ResourcePermission.OWNER,
        permissionId = "permId2",
        user =
            UserWithAvatar(
                userId = "usId",
                firstName = "first",
                lastName = "last",
                userName = "uName",
                isDisabled = false,
                avatarUrl = null,
            ),
    )

internal val RESOURCE_TAGS =
    listOf(
        TagModel(id = "id1", slug = "tag1", isShared = false),
        TagModel(id = "id2", slug = "tag2", isShared = false),
    )
