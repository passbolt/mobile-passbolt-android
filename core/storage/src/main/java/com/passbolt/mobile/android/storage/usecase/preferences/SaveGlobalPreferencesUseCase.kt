package com.passbolt.mobile.android.storage.usecase.preferences

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.storage.encrypted.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.storage.usecase.GLOBAL_PREFERENCES_FILE_NAME
import com.passbolt.mobile.android.storage.usecase.KEY_DEBUG_LOGS_ENABLED

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

class SaveGlobalPreferencesUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : UseCase<SaveGlobalPreferencesUseCase.Input, Unit> {

    override fun execute(input: Input) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$GLOBAL_PREFERENCES_FILE_NAME.xml")
        with(sharedPreferences.edit()) {
            putBoolean(KEY_DEBUG_LOGS_ENABLED, input.areDebugLogsEnabled)
            apply()
        }
    }

    data class Input(
        val areDebugLogsEnabled: Boolean
    )
}
