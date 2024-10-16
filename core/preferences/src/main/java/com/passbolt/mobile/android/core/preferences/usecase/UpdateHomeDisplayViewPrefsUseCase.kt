package com.passbolt.mobile.android.core.preferences.usecase

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.core.accounts.usecase.AccountPreferencesFileName
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.entity.home.HomeDisplayView
import com.passbolt.mobile.android.ui.DefaultFilterModel

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

class UpdateHomeDisplayViewPrefsUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : UseCase<UpdateHomeDisplayViewPrefsUseCase.Input, Unit>,
    SelectedAccountUseCase {

    override fun execute(input: Input) {
        val fileName = AccountPreferencesFileName(selectedAccountId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")

        with(sharedPreferences.edit()) {
            input.lastUsedHomeView?.let { putInt(KEY_LAST_USED_HOME_VIEW, it.ordinal) }
            input.userSetHomeView?.let { putInt(KEY_USER_SET_HOME_VIEW, it.ordinal) }
            apply()
        }
    }

    data class Input(
        val lastUsedHomeView: HomeDisplayView? = null,
        val userSetHomeView: DefaultFilterModel? = null
    )
}
