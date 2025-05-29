package com.passbolt.mobile.android.core.authenticationcore.session

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.core.accounts.usecase.SessionFileName
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory

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

class SaveSessionUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : UseCase<SaveSessionUseCase.Input, Unit> {
    override fun execute(input: Input) {
        val fileName = SessionFileName(input.userId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")
        with(sharedPreferences.edit()) {
            putString(ACCESS_TOKEN_KEY, input.accessToken)
            putString(REFRESH_TOKEN_KEY, input.refreshToken)
            putString(MFA_TOKEN_KEY, input.mfaToken)
            apply()
        }
    }

    data class Input(
        val userId: String,
        val refreshToken: String,
        val accessToken: String,
        val mfaToken: String? = null,
    )
}
