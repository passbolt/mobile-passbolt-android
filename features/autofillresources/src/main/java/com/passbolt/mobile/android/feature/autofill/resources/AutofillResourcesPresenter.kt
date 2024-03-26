package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.storage.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

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
class AutofillResourcesPresenter(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<AutofillResourcesContract.View>(coroutineLaunchContext),
    AutofillResourcesContract.Presenter {

    override var view: AutofillResourcesContract.View? = null

    private var uri: String? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun argsReceived(uri: String?, isRecreated: Boolean) {
        this.uri = uri
        if (!isRecreated) {
            if (getAccountsUseCase.execute(Unit).users.isNotEmpty()) {
                view?.navigateToAuth()
            } else {
                view?.navigateToSetup()
            }
        }
    }

    override fun userAuthenticated() {
        view?.navigateToAutofillHome()
        fullDataRefreshExecutor.performFullDataRefresh()
    }

    override fun itemClick(resourceModel: ResourceModel) {
        view?.showProgress()
        val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get {
            parametersOf(resourceModel, needSessionRefreshFlow, sessionRefreshedFlow)
        }
        scope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.providePassword() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnSuccess = {
                    view?.autofillReturn(
                        resourceModel.username.orEmpty(),
                        it.result,
                        uri
                    )
                }
            )
            view?.hideProgress()
        }
    }

    override fun newResourceCreated(resourceId: String) {
        scope.launch {
            itemClick(
                getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceId)).resource
            )
        }
    }
}
