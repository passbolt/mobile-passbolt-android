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

package com.passbolt.mobile.android.otpmoremenu.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.core.resourcetypes.graph.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.ui.OtpMoreMenuModel
import com.passbolt.mobile.android.ui.ResourcePermission
import java.util.UUID

class CreateOtpMoreMenuModelUseCase(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val resourceTypesUpdatesAdjacencyGraph: ResourceTypesUpdatesAdjacencyGraph,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider
) :
    AsyncUseCase<CreateOtpMoreMenuModelUseCase.Input, CreateOtpMoreMenuModelUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(input.resourceId)).resource
        val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[UUID.fromString(resource.resourceTypeId)]
        val updateActionsMetadata = resourceTypesUpdatesAdjacencyGraph.getUpdateActionsMetadata(requireNotNull(slug))

        return Output(
            OtpMoreMenuModel(
                title = resource.metadataJsonModel.name,
                canDelete = resource.permission in WRITE_PERMISSIONS,
                canEdit = resource.permission in WRITE_PERMISSIONS &&
                        updateActionsMetadata.any { it.action == UpdateAction.EDIT_TOTP }
            )
        )
    }

    data class Input(
        val resourceId: String
    )

    data class Output(
        val otpMoreMenuModel: OtpMoreMenuModel
    )

    private companion object {
        private val WRITE_PERMISSIONS = setOf(
            ResourcePermission.OWNER,
            ResourcePermission.UPDATE
        )
    }
}
