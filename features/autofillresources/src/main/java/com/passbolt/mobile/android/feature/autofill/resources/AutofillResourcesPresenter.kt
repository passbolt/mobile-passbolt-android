package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.storage.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val secretInteractor: SecretInteractor,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<AutofillResourcesContract.View>(coroutineLaunchContext),
    AutofillResourcesContract.Presenter {

    override var view: AutofillResourcesContract.View? = null

    private var uri: String? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: AutofillResourcesContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        if (getAccountsUseCase.execute(Unit).users.isNotEmpty()) {
            view.navigateToAuth()
        } else {
            view.navigateToSetup()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun argsReceived(uri: String?) {
        this.uri = uri
    }

    override fun userAuthenticated() {
        view?.navigateToAutofillHome()
        performFullDataRefresh()
    }

    override fun performFullDataRefresh() {
        with(fullDataRefreshExecutor) {
            this.attach(this@AutofillResourcesPresenter as BaseAuthenticatedPresenter<BaseAuthenticatedContract.View>)
            this.performFullDataRefresh()
        }
    }

    override fun itemClick(resourceModel: ResourceModel) {
        view?.showProgress()
        scope.launch {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)
            doAfterFetchAndDecrypt(
                resourceModel.resourceId,
                successAction = {
                    view?.hideProgress()
                    val password = secretParser.extractPassword(resourceTypeEnum, it)
                    view?.autofillReturn(resourceModel.username.orEmpty(), password, uri)
                },
                errorAction = {
                    view?.hideProgress()
                    view?.showError(it)
                })
        }
    }

    private suspend fun doAfterFetchAndDecrypt(
        resourceId: String,
        successAction: (ByteArray) -> Unit,
        errorAction: (String?) -> Unit
    ) {
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resourceId)
            }
        ) {
            is SecretInteractor.Output.DecryptFailure -> errorAction.invoke(output.error.message)
            is SecretInteractor.Output.FetchFailure -> errorAction.invoke(output.exception.message)
            is SecretInteractor.Output.Success -> successAction(output.decryptedSecret)
            is SecretInteractor.Output.Unauthorized -> {
                // can be ignored - runAuthenticatedOperation handles it
                Timber.d("Unauthorized during decrypting secret")
            }
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
