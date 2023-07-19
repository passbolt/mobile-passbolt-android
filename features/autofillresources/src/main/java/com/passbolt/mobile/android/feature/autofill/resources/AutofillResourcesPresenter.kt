package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.storage.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
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
        scope.launch {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)
            doAfterFetchAndDecrypt(
                resourceModel.resourceId,
                successAction = {
                    view?.hideProgress()
                    when (val password = secretParser.extractPassword(resourceTypeEnum, it)) {
                        is DecryptedSecretOrError.DecryptedSecret -> view?.autofillReturn(
                            resourceModel.username.orEmpty(),
                            password.secret,
                            uri
                        )
                        is DecryptedSecretOrError.Error -> error(password.message)
                    }
                },
                errorAction = { error(it) })
        }
    }

    private fun error(messahe: String?) {
        view?.hideProgress()
        view?.showError(messahe)
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
