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

package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.ui.formatter.DateFormatter
import com.passbolt.mobile.android.core.ui.formatter.FingerprintFormatter
import com.passbolt.mobile.android.core.users.user.FetchCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.GpgKeyModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KeyInspectorPresenter(
    private val fetchCurrentUserUseCase: FetchCurrentUserUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val dateFormatter: DateFormatter,
    private val fingerprintFormatter: FingerprintFormatter,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<KeyInspectorContract.View>(coroutineLaunchContext), KeyInspectorContract.Presenter {

    override var view: KeyInspectorContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var keyData: GpgKeyModel

    override fun attach(view: KeyInspectorContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        scope.launch {
            view.showProgress()
            showAccountData()
            fetchKeyData(view)
            view.hideProgress()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancel()
        super<BaseAuthenticatedPresenter>.detach()
    }

    private fun showAccountData() {
        val accountData = getSelectedAccountDataUseCase.execute(Unit)
        val defaultLabel = AccountModelMapper.defaultLabel(accountData.firstName, accountData.lastName)
        view?.showAvatar(accountData.avatarUrl)
        view?.showLabel(accountData.label ?: defaultLabel)
    }

    private suspend fun fetchKeyData(view: KeyInspectorContract.View) {
        when (val userResult = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            fetchCurrentUserUseCase.execute(Unit)
        }) {
            is FetchCurrentUserUseCase.Output.Failure<*> -> view.showError(userResult.message)
            is FetchCurrentUserUseCase.Output.Success -> {
                this.keyData = userResult.userModel.gpgKey
                with(userResult.userModel.gpgKey) {
                    view.showFingerprint(
                        fingerprintFormatter.format(fingerprint, appendMiddleSpacing = false).orEmpty()
                    )
                    view.showLength(bits.toString())

                    uid?.let { view.showUid(it) }
                    keyCreationDate?.let { view.showCreationDate(dateFormatter.format(it)) }
                    keyExpirationDate?.let { view.showExpirationDate(dateFormatter.format(it)) }
                    type?.let { view.showAlgorithm(it) }
                }
            }
        }
    }

    override fun uidCopyClick() {
        if (::keyData.isInitialized && !keyData.uid.isNullOrBlank()) {
            view?.addToClipboard(UID_LABEL, keyData.uid!!)
        }
    }

    override fun fingerprintCopyClick() {
        if (::keyData.isInitialized && keyData.fingerprint.isNotBlank()) {
            view?.addToClipboard(FINGERPRINT_LABEL, keyData.fingerprint)
        }
    }

    private companion object {
        private const val UID_LABEL = "Key UID"
        private const val FINGERPRINT_LABEL = "Key fingerprint"
    }
}
