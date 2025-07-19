package com.passbolt.mobile.android.feature.authentication.accountslist

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.ui.AccountModelUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val accountModelMapper: AccountModelMapper,
    private val removeAllAccountDataUseCase: RemoveAllAccountDataUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val databaseProvider: DatabaseProvider,
    coroutineLaunchContext: CoroutineLaunchContext,
) : AccountsListContract.Presenter {
    override var view: AccountsListContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var accounts: List<AccountModelUi> = emptyList()
    private var selectedAccountHasBeenRemoved = false

    override fun attach(view: AccountsListContract.View) {
        super.attach(view)
        displayAccounts()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    private fun displayAccounts() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        accounts =
            accountModelMapper.map(
                getAllAccountsDataUseCase.execute(Unit).accounts,
                selectedAccount,
            )
        view?.showAccounts(accounts)
    }

    override fun accountItemClick(model: AccountModelUi.AccountModel) {
        scope.launch {
            val currentAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
            if (currentAccount != null && currentAccount != model.userId) {
                view?.showProgress()
                signOutUseCase.execute(Unit)
                view?.hideProgress()
                // save selected account has to after signing out the user
                saveSelectedAccountUseCase.execute(UserIdInput(model.userId))
                saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(model.url))
                view?.notifySelectedAccountChanged()
                view?.navigateToNewAccountSignIn(model)
            } else {
                saveSelectedAccountUseCase.execute(UserIdInput(model.userId))
                saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(model.url))
                view?.navigateToSignIn(model)
            }
        }
    }

    override fun addAccountClick() {
        view?.navigateToSetup()
    }

    override fun removeAnAccountClick() {
        removeModeOn(isOn = true)
    }

    override fun doneRemovingAccountsClick() {
        removeModeOn(isOn = false)
    }

    private fun removeModeOn(isOn: Boolean) {
        accounts =
            accounts
                .filterIsInstance<AccountModelUi.AccountModel>()
                .map { it.copy(isTrashIconVisible = isOn) }
                .let {
                    if (!isOn) it + AccountModelUi.AddNewAccount else it
                }
        view?.showAccounts(accounts)

        if (isOn) {
            view?.apply {
                hideRemoveAccounts()
                showDoneRemovingAccounts()
            }
        } else {
            view?.apply {
                hideDoneRemovingAccounts()
                showRemoveAccounts()
            }
        }
    }

    override fun removeAccountClick(model: AccountModelUi.AccountModel) {
        view?.showRemoveAccountConfirmationDialog(model)
    }

    override fun confirmRemoveAccountClick(model: AccountModelUi.AccountModel) {
        val accountToRemoveId = model.userId
        view?.showProgress()
        scope.launch {
            getSelectedAccountUseCase.execute(Unit).selectedAccount?.let { selectedAccount ->
                if (accountToRemoveId == selectedAccount) {
                    signOutUseCase.execute(Unit)
                    selectedAccountHasBeenRemoved = true
                }
            }
            removeAllAccountDataUseCase.execute(UserIdInput(accountToRemoveId))
            databaseProvider.delete(accountToRemoveId)

            view?.hideProgress()
            view?.showAccountRemovedSnackbar()
            displayAccounts()
            removeModeOn(isOn = true)

            if (accounts.isEmpty()) {
                view?.navigateToStartUp()
            }
        }
    }

    override fun backClick() {
        view?.navigateBack(!selectedAccountHasBeenRemoved)
    }
}
