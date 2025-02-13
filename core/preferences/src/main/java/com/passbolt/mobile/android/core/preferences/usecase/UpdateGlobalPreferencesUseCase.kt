package com.passbolt.mobile.android.core.preferences.usecase

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

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

class UpdateGlobalPreferencesUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : UseCase<UpdateGlobalPreferencesUseCase.Input, Unit> {

    override fun execute(input: Input) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$GLOBAL_PREFERENCES_FILE_NAME.xml")
        with(sharedPreferences.edit()) {
            input.areDebugLogsEnabled?.let {
                putBoolean(KEY_DEBUG_LOGS_ENABLED, it)
            }
            input.debugLogFileCreationDateTime?.let {
                putLong(KEY_DEBUG_LOGS_FILE_CREATION_DATE_TIME, it.toEpochSecond(ZoneOffset.UTC))
            }
            input.isDeveloperModeEnabled?.let {
                putBoolean(KEY_IS_DEVELOPER_MODE_ENABLED, it)
            }
            input.isHideRootDialogEnabled?.let {
                putBoolean(KEY_IS_HIDE_ROOT_DIALOG_ENABLED, it)
            }
            apply()
        }
    }

    data class Input(
        val areDebugLogsEnabled: Boolean? = null,
        val debugLogFileCreationDateTime: LocalDateTime? = null,
        val isDeveloperModeEnabled: Boolean? = null,
        val isHideRootDialogEnabled: Boolean? = null
    )
}
