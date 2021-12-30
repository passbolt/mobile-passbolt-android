package com.passbolt.mobile.android.feature.accountdetails.screen

import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
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

class AccountDetailsPresenter(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AccountDetailsContract.Presenter,
    BaseAuthenticatedPresenter<AccountDetailsContract.View>(coroutineLaunchContext) {

    override var view: AccountDetailsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var label: String

    override fun attach(view: AccountDetailsContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        displayAccountData()
    }

    private fun displayAccountData() {
        scope.launch {
            val data = getSelectedAccountDataUseCase.execute(Unit)
            label = data.label ?: AccountModelMapper.defaultLabel(data.firstName, data.lastName)

            view?.apply {
                showEmail(data.email.orEmpty())
                showName("${data.firstName.orEmpty()} ${data.lastName.orEmpty()}")
                showOrgUrl(data.url)
                showAvatar(data.avatarUrl)
                showLabel(label)
            }
        }
    }

    override fun labelInputChanged(label: String) {
        this.label = label
    }

    override fun saveClick() {
        view?.clearValidationErrors()
        validation {
            of(label) {
                withRules(StringNotBlank, StringMaxLength(LABEL_MAX_LENGTH))
                onInvalid { view?.showLabelLengthError(LABEL_MAX_LENGTH) }
            }
            onValid { updateAccountData() }
        }
    }

    private fun updateAccountData() {
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount),
                label = this.label
            )
        )
        view?.showLabelChanged()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    private companion object {
        private const val LABEL_MAX_LENGTH = 25
    }
}
