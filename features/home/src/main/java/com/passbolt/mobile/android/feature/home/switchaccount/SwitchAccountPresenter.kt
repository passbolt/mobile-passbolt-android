package com.passbolt.mobile.android.feature.home.switchaccount

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.mappers.SwitchAccountModelMapper
import com.passbolt.mobile.android.storage.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.ui.SwitchAccountUiModel
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

class SwitchAccountPresenter(
    private val getAllAccountsDataUseCase: GetAllAccountsDataUseCase,
    private val switchAccountModelMapper: SwitchAccountModelMapper,
    private val signOutUseCase: SignOutUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : SwitchAccountContract.Presenter {

    override var view: SwitchAccountContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var appContext: AppContext

    override fun argsRetrieved(appContext: AppContext) {
        this.appContext = appContext
        view?.showAccountsList(
            prepareAccountList()
        )
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    override fun viewResumed() {
        view?.showAccountsList(
            prepareAccountList()
        )
    }

    private fun prepareAccountList(): List<SwitchAccountUiModel> =
        switchAccountModelMapper
            .map(getAllAccountsDataUseCase.execute(Unit).accounts, appContext)

    override fun signOutClick() {
        view?.showSignOutDialog()
    }

    override fun signOutConfirmed() {
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.navigateToStartup()
        }
    }

    override fun switchAccountClick(account: SwitchAccountUiModel.AccountItem) {
        view?.navigateToSignInForAccount(account.userId)
    }

    override fun seeDetailsClick() {
        view?.navigateToAccountDetails()
    }
}
