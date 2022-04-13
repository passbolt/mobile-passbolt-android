package com.passbolt.mobile.android.database.impl.resourcetypes

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.entity.resource.ResourceField
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
class GetResourceTypeWithFieldsBySlugUseCase(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : AsyncUseCase<GetResourceTypeWithFieldsBySlugUseCase.Input, GetResourceTypeWithFieldsBySlugUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val resourceType = databaseProvider
            .get(userId)
            .resourceTypesDao()
            .getResourceTypeWithFieldsBySlug(input.slug)
        return Output(
            resourceType.resourceType.resourceTypeId,
            resourceType.resourceFields
        )
    }

    data class Input(
        val slug: String = DEFAULT_SLUG
    )

    data class Output(
        val resourceTypeId: String,
        val fields: List<ResourceField>
    )

    companion object {
        private const val DEFAULT_SLUG = "password-and-description"
    }
}
