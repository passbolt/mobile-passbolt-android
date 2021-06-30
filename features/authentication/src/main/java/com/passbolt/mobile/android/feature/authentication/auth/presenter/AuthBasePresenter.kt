package com.passbolt.mobile.android.feature.authentication.auth.presenter

import androidx.annotation.CallSuper
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.storage.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
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

// base presenter for auth view
// handles account details display and forgot password dialog
abstract class AuthBasePresenter(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthContract.Presenter {

    override var view: AuthContract.View? = null

    private val job = SupervisorJob()
    protected val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    protected lateinit var userId: String

    override fun attach(view: AuthContract.View) {
        super.attach(view)
        view.showTitle()
    }

    override fun argsRetrieved(userId: String) {
        this.userId = userId
        getAccountData()
    }

    private fun getAccountData() {
        scope.launch {
            val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
            // TODO load cached avatar
            view?.showName("${accountData.firstName.orEmpty()} ${accountData.lastName.orEmpty()}")
            accountData.email?.let { view?.showEmail(it) }
            accountData.avatarUrl?.let { view?.showAvatar(it) }
        }
    }

    override fun forgotPasswordClick() {
        view?.showForgotPasswordDialog()
    }

    override fun passphraseInputIsEmpty(isEmpty: Boolean) {
        if (isEmpty) {
            view?.disableAuthButton()
        } else {
            view?.enableAuthButton()
        }
    }

    override fun backClick() {
        view?.navigateBack()
    }

    @CallSuper
    override fun signInClick(passphrase: CharArray?) {
        view?.hideKeyboard()
    }
}
