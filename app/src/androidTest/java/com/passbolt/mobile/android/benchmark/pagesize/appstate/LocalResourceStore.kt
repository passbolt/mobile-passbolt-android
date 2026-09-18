/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.benchmark.pagesize.appstate

import androidx.room3.useReaderConnection
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.domain.resources.usecase.db.RemoveLocalResourcesWithUpdateStateUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.SetLocalResourcesUpdateStateUseCase
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState.PENDING
import kotlinx.coroutines.runBlocking

class LocalResourceStore(
    private val setLocalResourcesUpdateStateUseCase: SetLocalResourcesUpdateStateUseCase,
    private val removeLocalResourcesWithUpdateStateUseCase: RemoveLocalResourcesWithUpdateStateUseCase,
    private val databaseProvider: DatabaseProvider,
    private val userId: String,
) {
    fun clear() =
        runBlocking {
            setLocalResourcesUpdateStateUseCase.execute(SetLocalResourcesUpdateStateUseCase.Input(PENDING))
            removeLocalResourcesWithUpdateStateUseCase.execute(RemoveLocalResourcesWithUpdateStateUseCase.Input(PENDING))
        }

    fun count(): Int =
        runBlocking {
            databaseProvider
                .get(userId)
                .useReaderConnection { transactor ->
                    transactor.usePrepared(COUNT_RESOURCES) { statement ->
                        if (statement.step()) statement.getInt(0) else 0
                    }
                }
        }

    private companion object {
        private const val COUNT_RESOURCES = "SELECT COUNT(*) FROM Resource"
    }
}
