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

package com.passbolt.mobile.android.resourcemoremenu.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.isFavourite
import java.util.UUID

class CreateResourceMoreMenuModelUseCase(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider
) :
    AsyncUseCase<CreateResourceMoreMenuModelUseCase.Input, CreateResourceMoreMenuModelUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(input.resourceId)).resource
        val copyRbac = getRbacRulesUseCase.execute(Unit).rbacModel.passwordCopyRule
        val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[UUID.fromString(resource.resourceTypeId)]
        val contentType = ContentType.fromSlug(slug!!)

        return Output(
            ResourceMoreMenuModel(
                title = resource.metadataJsonModel.name,
                canCopy = copyRbac == ALLOW,
                canDelete = resource.permission in WRITE_PERMISSIONS,
                canEdit = resource.permission in WRITE_PERMISSIONS,
                canShare = resource.permission == ResourcePermission.OWNER,
                favouriteOption = if (resource.isFavourite()) {
                    REMOVE_FROM_FAVOURITES
                } else {
                    ADD_TO_FAVOURITES
                },
                descriptionOptions = buildList {
                    if (contentType.hasMetadataDescription()) {
                        add(HAS_METADATA_DESCRIPTION)
                    }
                    if (contentType.hasNote()) {
                        add(HAS_NOTE)
                    }
                }
            )
        )
    }

    data class Input(
        val resourceId: String
    )

    data class Output(
        val resourceMenuModel: ResourceMoreMenuModel
    )

    private companion object {
        private val WRITE_PERMISSIONS = setOf(
            ResourcePermission.OWNER,
            ResourcePermission.UPDATE
        )
    }
}
