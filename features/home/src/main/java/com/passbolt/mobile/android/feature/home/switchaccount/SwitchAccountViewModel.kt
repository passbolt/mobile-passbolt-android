package com.passbolt.mobile.android.feature.home.switchaccount

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.Close
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.CloseSignOutDialog
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.Initialize
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.ManageAccounts
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.SeeCurrentAccountDetails
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.SignOut
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.SignOutConfirmed
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountIntent.SwitchAccount
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountSideEffect.NavigateToSignInForAccount
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountSideEffect.NavigateToStartup
import com.passbolt.mobile.android.mappers.SwitchAccountModelMapper
import com.passbolt.mobile.android.ui.SwitchAccountUiModel.AccountItem
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

class SwitchAccountViewModel(
    private val getAllAccountsDataUseCase: GetAllAccountsDataUseCase,
    private val switchAccountModelMapper: SwitchAccountModelMapper,
    private val signOutUseCase: SignOutUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : SideEffectViewModel<SwitchAccountState, SwitchAccountSideEffect>(SwitchAccountState()) {
    val appContext: AppContext
        get() = requireNotNull(viewState.value.appContext) { "App context was not initialized" }

    fun onIntent(intent: SwitchAccountIntent) {
        when (intent) {
            is Close -> emitSideEffect(SwitchAccountSideEffect.Dismiss)
            is Initialize -> initialize(intent)
            is SeeCurrentAccountDetails -> emitSideEffect(SwitchAccountSideEffect.NavigateToAccountDetails)
            is SignOut -> updateViewState { copy(showSignOutDialog = true) }
            is SignOutConfirmed -> signOut()
            is SwitchAccount -> switchToAccount(intent.account)
            ManageAccounts -> emitSideEffect(SwitchAccountSideEffect.NavigateToManageAccounts)
            CloseSignOutDialog -> updateViewState { copy(showSignOutDialog = false) }
        }
    }

    private fun initialize(initialize: Initialize) {
        updateViewState { copy(appContext = initialize.appContext) }

        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        val accounts = getAllAccountsDataUseCase.execute(Unit).accounts
        val accountsList = switchAccountModelMapper.map(accounts, selectedAccount, initialize.appContext)

        updateViewState {
            copy(accountsList = accountsList)
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            updateViewState { copy(showSignOutDialog = false, showProgress = true) }
            dataRefreshTrackingFlow.awaitIdle()
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(NavigateToStartup(appContext))
        }
    }

    private fun switchToAccount(account: AccountItem) {
        saveSelectedAccountUseCase.execute(UserIdInput(account.userId))
        emitSideEffect(NavigateToSignInForAccount(appContext))
    }
}
