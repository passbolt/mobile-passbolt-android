package com.passbolt.mobile.android.database.impl.resourcetypes

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.entity.resource.ResourceTypesAndFieldsCrossRef
import com.passbolt.mobile.android.mappers.ResourceTypesModelMapper
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase

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
class AddLocalResourceTypesUseCase(
    private val databaseProvider: DatabaseProvider,
    private val resourceTypesModelMapper: ResourceTypesModelMapper,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : AsyncUseCase<AddLocalResourceTypesUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)

        val resourceTypesDao = databaseProvider
            .get(selectedAccount)
            .resourceTypesDao()

        val resourceFieldsDao = databaseProvider
            .get(selectedAccount)
            .resourceFieldsDao()

        val resourceTypesAndFieldsCrossRefDao = databaseProvider
            .get(selectedAccount)
            .resourceTypesAndFieldsCrossRefDao()

        val resourceTypeDbModel = resourceTypesModelMapper
            .map(input.resourceTypesDto)

        // insert each resource types along with its fields and ResourceType<->ResourceField cross reference
        resourceFieldsDao.deleteAll()
        resourceTypesAndFieldsCrossRefDao.deleteAll()
        resourceTypeDbModel.forEach { resourceType ->
            resourceTypesDao.insert(resourceType.resourceType)
            resourceType.resourceFields.forEach { resourceTypeField ->
                val resourceTypeFieldId = resourceFieldsDao.insert(resourceTypeField)
                resourceTypesAndFieldsCrossRefDao.insert(
                    ResourceTypesAndFieldsCrossRef(
                        resourceType.resourceType.resourceTypeId,
                        resourceTypeFieldId
                    )
                )
            }
        }
    }

    data class Input(
        val resourceTypesDto: List<ResourceTypeDto>
    )
}
