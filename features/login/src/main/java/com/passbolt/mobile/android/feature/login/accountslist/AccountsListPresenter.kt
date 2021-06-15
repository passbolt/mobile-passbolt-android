package com.passbolt.mobile.android.feature.login.accountslist

import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.storage.usecase.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.ui.AccountModelUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
class AccountsListPresenter(
    private val getAllAccountsDataUseCase: GetAllAccountsDataUseCase,
    private val accountModelMapper: AccountModelMapper,
    coroutineLaunchContext: CoroutineLaunchContext
) : AccountsListContract.Presenter {

    override var view: AccountsListContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: AccountsListContract.View) {
        super.attach(view)
        view.showAccounts(
            listOf(
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png",
                    isFirstItem = true
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AccountModel(
                    "First name",
                    "email@passbolt.com",
                    "https://image.pngaaa.com/569/2189569-middle.png"
                ),
                AccountModelUi.AddNewAccount
            )
        )
        // TODO display account from use case
        // displayAccounts()
    }

    private fun displayAccounts() {
        scope.launch {
            val accounts = accountModelMapper.map(getAllAccountsDataUseCase.execute(Unit).accounts)
            view?.showAccounts(accounts)
        }
    }

    override fun accountItemClick(model: AccountModelUi.AccountModel) {
        // TODO
    }

    override fun addAccountClick() {
        // TODO
    }
}
