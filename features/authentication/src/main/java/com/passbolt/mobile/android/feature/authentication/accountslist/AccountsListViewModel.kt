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

package com.passbolt.mobile.android.feature.authentication.accountslist

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.ManageAccount
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.AddAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.ConfirmRemoveAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.DismissRemoveAccountDialog
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.EnterRemoveAccountMode
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.ExitRemoveAccountMode
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.GoBack
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.RemoveAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.SelectAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToNewAccountSignIn
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.ShowSuccessSnackBar
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.SnackBarType.ACCOUNT_REMOVED
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.ui.AccountModelUi.AccountModel

class AccountsListViewModel(
    private val authConfig: AuthConfig,
    private val getAllAccountsDataUseCase: GetAllAccountsDataUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val accountModelMapper: AccountModelMapper,
    private val removeAllAccountDataUseCase: RemoveAllAccountDataUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val databaseProvider: DatabaseProvider,
) : SideEffectViewModel<AccountsListState, AccountsListSideEffect>(
        AccountsListState(
            showManageAccountsTopBar = authConfig is ManageAccount,
            showHeader = authConfig !is ManageAccount,
        ),
    ) {
    init {
        loadAccounts()
    }

    fun onIntent(intent: AccountsListIntent) {
        when (intent) {
            is SelectAccount -> selectAccount(intent.account)
            AddAccount -> emitSideEffect(AccountsListSideEffect.NavigateToSetup)
            EnterRemoveAccountMode -> setRemoveMode(isOn = true)
            ExitRemoveAccountMode -> setRemoveMode(isOn = false)
            is RemoveAccount -> updateViewState { copy(accountToRemove = intent.account, showAccountRemovalConfirmation = true) }
            DismissRemoveAccountDialog -> updateViewState { copy(showAccountRemovalConfirmation = false) }
            is ConfirmRemoveAccount -> removeAccount(intent.account)
            GoBack -> goBack()
        }
    }

    private fun loadAccounts() {
        val currentUserId = getSelectedAccountUseCase.execute(Unit).selectedAccount
        val accounts = accountModelMapper.map(getAllAccountsDataUseCase.execute(Unit).accounts)
        updateViewState { copy(accounts = accounts, currentUserId = currentUserId) }
    }

    private fun selectAccount(account: AccountModel) {
        launch {
            val currentAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
            if (currentAccount != null && currentAccount != account.userId) {
                updateViewState { copy(showProgress = true) }
                signOutUseCase.execute(Unit)
                saveSelectedAccountUseCase.execute(UserIdInput(account.userId))
                saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(account.url))
                updateViewState { copy(showProgress = false, selectedAccountChanged = true) }
                emitSideEffect(NavigateToNewAccountSignIn(account))
            } else {
                saveSelectedAccountUseCase.execute(UserIdInput(account.userId))
                saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(account.url))
                emitSideEffect(NavigateToSignIn(account))
            }
        }
    }

    private fun setRemoveMode(isOn: Boolean) {
        updateViewState { copy(isRemoveMode = isOn) }
    }

    private fun removeAccount(account: AccountModel) {
        updateViewState { copy(showAccountRemovalConfirmation = false, showProgress = true) }
        launch {
            getSelectedAccountUseCase.execute(Unit).selectedAccount?.let { selectedAccount ->
                if (account.userId == selectedAccount) {
                    signOutUseCase.execute(Unit)
                    updateViewState { copy(selectedAccountRemoved = true) }
                }
            }
            removeAllAccountDataUseCase.execute(UserIdInput(account.userId))
            databaseProvider.delete(account.userId)

            updateViewState { copy(showProgress = false) }
            emitSideEffect(ShowSuccessSnackBar(ACCOUNT_REMOVED))
            loadAccounts()
            setRemoveMode(isOn = true)

            if (viewState.value.accounts
                    .filterIsInstance<AccountModel>()
                    .isEmpty()
            ) {
                emitSideEffect(AccountsListSideEffect.NavigateToStartUp)
            }
        }
    }

    private fun goBack() {
        val state = viewState.value
        if (authConfig is ManageAccount) {
            if (!state.selectedAccountRemoved && !state.selectedAccountChanged) {
                emitSideEffect(AccountsListSideEffect.Finish)
            } else {
                emitSideEffect(AccountsListSideEffect.FinishAffinity)
            }
        } else {
            emitSideEffect(AccountsListSideEffect.FinishAffinity)
        }
    }
}
