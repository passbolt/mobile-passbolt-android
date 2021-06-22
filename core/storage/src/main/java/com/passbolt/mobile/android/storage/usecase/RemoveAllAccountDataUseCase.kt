package com.passbolt.mobile.android.storage.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import timber.log.Timber

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
class RemoveAllAccountDataUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeAccountDataUseCase: RemoveAccountDataUseCase,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val removePrivateKeyUseCase: RemovePrivateKeyUseCase,
    private val removeSelectedAccountUseCase: RemoveSelectedAccountUseCase,
    private val removeSessionUseCase: RemoveSessionUseCase,
    private val removeUserAvatarUseCase: RemoveUserAvatarUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase
) : AsyncUseCase<UserIdInput, Unit> {

    override suspend fun execute(input: UserIdInput) {
        val accountToRemoveId = UserIdInput(input.userId)
        removeAccountData(accountToRemoveId)

        runCatching {
            val selectedAccountId = getSelectedAccountUseCase.execute(Unit).selectedAccount

            if (accountToRemoveId.userId == selectedAccountId) {
                removeSelectedAccountUseCase.execute(accountToRemoveId)
            }
        }
            .onFailure {
                // ignore removing selected account id when no account is selected
                Timber.d(it)
            }
    }

    private fun removeAccountData(userIdInput: UserIdInput) {
        removeAccountDataUseCase.execute(userIdInput)
        removePassphraseUseCase.execute(userIdInput)
        removePrivateKeyUseCase.execute(userIdInput)
        removeUserAvatarUseCase.execute(userIdInput)
        removeSessionUseCase.execute(userIdInput)
        removeAccountUseCase.execute(userIdInput)
    }
}
