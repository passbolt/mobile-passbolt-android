package com.passbolt.mobile.android.feature.login.accountslist

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
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
    private var accounts: List<AccountModelUi> = emptyList()

    override fun attach(view: AccountsListContract.View) {
        super.attach(view)
        displayAccounts()
    }

    private fun displayAccounts() {
        scope.launch {
            accounts = accountModelMapper.map(getAllAccountsDataUseCase.execute(Unit).accounts, true)
            view?.showAccounts(accounts)
        }
    }

    override fun accountItemClick(model: AccountModelUi.AccountModel) {
        view?.navigateToLogin(model)
    }

    override fun addAccountClick() {
        view?.navigateToSetup()
    }

    override fun removeAnAccountClick() {
        trashIconsVisible(true)
        view?.apply {
            hideRemoveAccounts()
            showDoneRemovingAccounts()
        }
    }

    private fun trashIconsVisible(areVisible: Boolean) {
        accounts = accounts.map {
            when (it) {
                is AccountModelUi.AccountModel -> it.copy(isTrashIconVisible = areVisible)
                is AccountModelUi.AddNewAccount -> it
            }
        }
        view?.showAccounts(accounts)
    }

    override fun doneRemovingAccountsClick() {
        trashIconsVisible(false)
        view?.apply {
            hideDoneRemovingAccounts()
            showRemoveAccounts()
        }
    }

    override fun removeAccountClick(model: AccountModelUi.AccountModel) {
        view?.showRemoveAccountConfirmationDialog(model)
    }

    override fun confirmRemoveAccountClick(model: AccountModelUi.AccountModel) {
        // TODO remove account data and show confirmation
    }
}
