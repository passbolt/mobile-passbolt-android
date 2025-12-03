package com.passbolt.mobile.android.core.resources.usecase.db

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState.UPDATED
import com.passbolt.mobile.android.mappers.ResourceModelMapper
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
class UpdateLocalResourceUseCase(
    private val databaseProvider: DatabaseProvider,
    private val resourceModelMapper: ResourceModelMapper,
) : AsyncUseCase<UpdateLocalResourceUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        val db = databaseProvider.get(selectedAccountId)
        val resourcesDao = db.resourcesDao()
        val resourceMetadataDao = db.resourceMetadataDao()
        val resourceUriDao = db.resourceUriDao()

        resourcesDao.update(resourceModelMapper.map(input.resourceModel, resourceUpdateState = UPDATED))
        resourceMetadataDao.updateMetadataForResource(
            resourceId = input.resourceModel.resourceId,
            metadataJson = requireNotNull(input.resourceModel.metadataJsonModel.json),
            name = input.resourceModel.metadataJsonModel.name,
            username = input.resourceModel.metadataJsonModel.username,
            description = input.resourceModel.metadataJsonModel.description,
            customFieldsKeys =
                input.resourceModel.metadataJsonModel.customFields
                    ?.joinToString(),
        )

        resourceUriDao.apply {
            deleteForResource(input.resourceModel.resourceId)
            insertAll(resourceModelMapper.mapResourceUris(input.resourceModel))
        }
    }

    data class Input(
        val resourceModel: ResourceModel,
    )
}
