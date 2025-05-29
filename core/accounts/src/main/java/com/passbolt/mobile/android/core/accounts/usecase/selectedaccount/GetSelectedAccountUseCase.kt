package com.passbolt.mobile.android.core.accounts.usecase.selectedaccount

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.core.accounts.usecase.SELECTED_ACCOUNT_ALIAS
import com.passbolt.mobile.android.core.accounts.usecase.SELECTED_ACCOUNT_KEY
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

class GetSelectedAccountUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : UseCase<Unit, GetSelectedAccountUseCase.Output> {
    override fun execute(input: Unit): Output {
        val sharedPreferences =
            encryptedSharedPreferencesFactory.get("$SELECTED_ACCOUNT_ALIAS.xml")
        val selectedAccount =
            sharedPreferences.getString(SELECTED_ACCOUNT_KEY, null)

        return if (selectedAccount != null) {
            Output(selectedAccount)
        } else {
            Output(null)
        }
    }

    data class Output(
        val selectedAccount: String?,
    )
}
