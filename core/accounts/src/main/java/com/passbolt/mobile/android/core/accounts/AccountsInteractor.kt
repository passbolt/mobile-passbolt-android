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

package com.passbolt.mobile.android.core.accounts

import com.passbolt.mobile.android.common.HttpsVerifier
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accounts.CheckAccountExistsUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel

typealias UserId = String

class AccountsInteractor(
    private val uuidProvider: UuidProvider,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val checkAccountExistsUseCase: CheckAccountExistsUseCase,
    private val httpsVerifier: HttpsVerifier
) {

    fun injectPredefinedAccountData(
        accountSetupData: AccountSetupDataModel,
        onSuccess: (UserId) -> Unit,
        onFailure: (InjectAccountFailureType) -> Unit
    ) {
        val userExistsResult =
            checkAccountExistsUseCase.execute(CheckAccountExistsUseCase.Input(accountSetupData.serverUserId))
        if (userExistsResult.exist) {
            onFailure(ACCOUNT_ALREADY_LINKED)
        } else if (!httpsVerifier.isHttps(accountSetupData.domain)) {
            onFailure(ERROR_NON_HTTPS_DOMAIN)
        } else {
            val userId = uuidProvider.get()
            saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(accountSetupData.domain))
            updateAccountDataUseCase.execute(
                UpdateAccountDataUseCase.Input(
                    userId = userId,
                    firstName = accountSetupData.firstName,
                    lastName = accountSetupData.lastName,
                    avatarUrl = accountSetupData.avatarUrl,
                    email = accountSetupData.userName,
                    url = accountSetupData.domain,
                    serverId = accountSetupData.serverUserId
                )
            )

            when (savePrivateKeyUseCase.execute(SavePrivateKeyUseCase.Input(userId, accountSetupData.armoredKey))) {
                SavePrivateKeyUseCase.Output.Failure -> {
                    onFailure(ERROR_WHEN_SAVING_PRIVATE_KEY)
                }
                SavePrivateKeyUseCase.Output.Success -> {
                    onSuccess(userId)
                }
            }
        }
    }

    enum class InjectAccountFailureType {
        ACCOUNT_ALREADY_LINKED,
        ERROR_WHEN_SAVING_PRIVATE_KEY,
        ERROR_NON_HTTPS_DOMAIN
    }
}
