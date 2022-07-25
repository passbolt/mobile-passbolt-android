package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.data.interactor.HomeDataInteractor
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.DataRefreshStatus
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.storage.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
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
    private val homeDataInteractor: HomeDataInteractor,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val secretInteractor: SecretInteractor,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<AutofillResourcesContract.View>(coroutineLaunchContext),
    AutofillResourcesContract.Presenter {

    override var view: AutofillResourcesContract.View? = null

    private var uri: String? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private val _dataRefreshStatusFlow = MutableSharedFlow<DataRefreshStatus>(replay = 1)
    override val dataRefreshFinishedStatusFlow: Flow<DataRefreshStatus.Finished> = _dataRefreshStatusFlow
        .filterIsInstance()

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

    override fun closeClick() {
        view?.navigateToHome()
    }

    override fun performFullDataRefresh() {
        scope.launch {
            Timber.d("Full data refresh initiated")
            val output = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                homeDataInteractor.refreshAllHomeScreenData()
            }
            _dataRefreshStatusFlow.emit(DataRefreshStatus.Finished(output))
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
