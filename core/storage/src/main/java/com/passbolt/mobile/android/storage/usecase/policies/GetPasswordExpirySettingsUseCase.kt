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

package com.passbolt.mobile.android.storage.usecase.policies

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.storage.KEY_AUTOMATIC_PASSWORD_EXPIRY
import com.passbolt.mobile.android.storage.KEY_AUTOMATIC_PASSWORD_UPDATE
import com.passbolt.mobile.android.storage.KEY_DEFAULT_EXPIRY_PERIOD_DAYS
import com.passbolt.mobile.android.storage.encrypted.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.storage.paths.PasswordExpirySettingsFileName
import com.passbolt.mobile.android.storage.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.ui.PasswordExpirySettings

class GetPasswordExpirySettingsUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : AsyncUseCase<Unit, GetPasswordExpirySettingsUseCase.Output>, SelectedAccountUseCase {

    override suspend fun execute(input: Unit): Output {
        val fileName = PasswordExpirySettingsFileName(selectedAccountId).name
        return encryptedSharedPreferencesFactory.get("$fileName.xml").let {
            val isAutomaticExpireEnabled =
                it.getBoolean(KEY_AUTOMATIC_PASSWORD_EXPIRY, DEFAULT_AUTOMATIC_PASSWORD_EXPIRY)
            val isAutomaticUpdateEnabled =
                it.getBoolean(KEY_AUTOMATIC_PASSWORD_UPDATE, DEFAULT_AUTOMATIC_PASSWORD_UPDATE)
            val defaultExpiryPeriodDays = if (it.contains(KEY_DEFAULT_EXPIRY_PERIOD_DAYS)) {
                it.getInt(KEY_DEFAULT_EXPIRY_PERIOD_DAYS, DEFAULT_EXPIRY_PERIOD_DAYS)
            } else {
                null
            }
            Output(
                PasswordExpirySettings(
                    automaticExpiry = isAutomaticExpireEnabled,
                    automaticUpdate = isAutomaticUpdateEnabled,
                    defaultExpiryPeriodDays = defaultExpiryPeriodDays
                )
            )
        }
    }

    data class Output(val expirySettings: PasswordExpirySettings)

    private companion object {
        const val DEFAULT_EXPIRY_PERIOD_DAYS = 90
        const val DEFAULT_AUTOMATIC_PASSWORD_EXPIRY = true
        const val DEFAULT_AUTOMATIC_PASSWORD_UPDATE = true
    }
}
