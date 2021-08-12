package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.entity.account.FolderEntity
import com.passbolt.mobile.android.entity.account.Permission
import com.passbolt.mobile.android.entity.account.ResourceEntity
import com.passbolt.mobile.android.entity.account.SecretTypeEntity
import com.passbolt.mobile.android.ui.ResourceModel

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
class ResourceModelMapper(
    private val initialsProvider: InitialsProvider
) {

    fun map(resource: ResourceResponseDto): ResourceModel =
        ResourceModel(
            resourceId = resource.id,
            name = resource.name,
            username = resource.username,
            initials = initialsProvider.get(resource.name),
            icon = null,
            url = resource.uri,
            searchCriteria = "${resource.name}${resource.username}${resource.uri}"
        )

    fun map(resourceModel: ResourceModel): ResourceEntity =
        ResourceEntity(
            resourceId = resourceModel.resourceId,
            resourceName = resourceModel.name,
            description = "",
            resourcePermission = Permission.READ,
            url = resourceModel.url,
            username = resourceModel.username,
            secretType = SecretTypeEntity(),
            folder = FolderEntity(name = "name", permission = Permission.READ, parentId = 0)
        )

    fun map(resourceEntity: ResourceEntity): ResourceModel =
        ResourceModel(
            resourceId = resourceEntity.resourceId,
            name = resourceEntity.resourceName,
            url = resourceEntity.url,
            username = resourceEntity.username,
            icon = null,
            initials = initialsProvider.get(resourceEntity.resourceName),
            searchCriteria = ""
        )
}
