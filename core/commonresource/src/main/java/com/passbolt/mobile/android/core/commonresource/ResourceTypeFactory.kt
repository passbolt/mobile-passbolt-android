package com.passbolt.mobile.android.core.commonresource

import com.passbolt.mobile.android.database.DatabaseProvider
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
class ResourceTypeFactory(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) {

    suspend fun getResourceTypeEnum(resourceTypeId: String): ResourceTypeEnum {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val resourceFields = databaseProvider.get(selectedAccount)
            .resourceTypesDao()
            .getResourceTypeWithFieldsById(resourceTypeId)
            .resourceFields

        val secretFields = resourceFields.filter { it.isSecret }.map { it.name }
        return if (secretFields.size == 2 && secretFields.containsAll(SECRET_WITH_DESCRIPTION_SECRET_FIELDS)) {
            ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
        } else if (secretFields.size == 1 && secretFields.contains(FIELD_SECRET)) {
            ResourceTypeEnum.SIMPLE_PASSWORD
        } else {
            throw UnsupportedOperationException(
                "Cannot parse resource type enum with secret fields " +
                        secretFields.joinToString(separator = ", ")
            )
        }
    }

    enum class ResourceTypeEnum {
        SIMPLE_PASSWORD,
        PASSWORD_WITH_DESCRIPTION
    }

    private companion object {
        private const val FIELD_SECRET = "secret"
        private val SECRET_WITH_DESCRIPTION_SECRET_FIELDS = listOf("password", "description")
    }
}
