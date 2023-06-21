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

package com.passbolt.mobile.android.serializers.gson

import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import java.util.UUID

class ResourceTypeIdToSlugMappingProvider(
    private val resourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) {

    private val perAccountMappings = mutableMapOf<String, Map<UUID, String>>()

    suspend fun provideMappingForSelectedAccount(): Map<UUID, String> {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return if (perAccountMappings.containsKey(selectedAccount)) {
            perAccountMappings[selectedAccount]!!
        } else {
            resourceTypeIdToSlugMappingUseCase.execute(Unit).idToSlugMapping.also {
                perAccountMappings[selectedAccount] = it
            }
        }
    }
}
