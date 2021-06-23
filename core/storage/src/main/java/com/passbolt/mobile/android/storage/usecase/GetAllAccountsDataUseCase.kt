package com.passbolt.mobile.android.storage.usecase

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.entity.account.AccountEntity
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput

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

class GetAllAccountsDataUseCase(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val getAccountsUseCase: GetAccountsUseCase
) : UseCase<Unit, GetAllAccountsDataUseCase.Output> {

    override fun execute(input: Unit): Output {
        val accountsData = getAccountsUseCase.execute(Unit).users.map { userId ->
            val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
            AccountEntity(
                userId = userId,
                firstName = accountData.firstName,
                lastName = accountData.lastName,
                email = accountData.email,
                avatarUrl = accountData.avatarUrl,
                url = accountData.url
            )
        }

        return Output(accountsData)
    }

    class Output(
        val accounts: List<AccountEntity>
    )
}