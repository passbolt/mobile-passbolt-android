package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.networking.BaseNetworkingPresenter
import com.passbolt.mobile.android.core.networking.session.runRequest
import com.passbolt.mobile.android.feature.home.screen.usecase.GetResourcesUseCase
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.PasswordModel
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
class HomePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val resourceModelMapper: ResourceModelMapper,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase
) : BaseNetworkingPresenter<HomeContract.View>(coroutineLaunchContext), HomeContract.Presenter {

    override var view: HomeContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var currentSearchText: String = ""
    private var allItemsList: List<PasswordModel> = emptyList()

    override fun attach(view: HomeContract.View) {
        super<BaseNetworkingPresenter>.attach(view)
        fetchPasswords()
        getSelectedAccountDataUseCase.execute(Unit).avatarUrl?.let {
            view.displayAvatar(it)
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseNetworkingPresenter>.detach()
    }

    override fun searchTextChange(text: String) {
        currentSearchText = text
        filterList()
    }

    private fun fetchPasswords() {
        scope.launch {
            when (val result =
                runRequest(needSessionRefreshFlow, sessionRefreshedFlow) { getResourcesUseCase.execute(Unit) }) {
                is GetResourcesUseCase.Output.Failure<*> -> {
                    view?.hideRefreshProgress()
                    view?.hideProgress()
                    view?.showError()
                }
                is GetResourcesUseCase.Output.Success -> {
                    view?.hideRefreshProgress()
                    view?.hideProgress()
                    allItemsList = result.resources.map { resourceModelMapper.map(it) }
                    displayPasswords()
                }
            }
        }
    }

    private fun displayPasswords() {
        if (allItemsList.isEmpty()) {
            view?.showEmptyList()
        } else {
            if (currentSearchText.isEmpty()) {
                view?.showPasswords(allItemsList)
            } else {
                filterList()
            }
        }
    }

    private fun filterList() {
        val filtered = allItemsList.filter {
            it.searchCriteria.contains(currentSearchText)
        }
        if (filtered.isEmpty()) {
            view?.showEmptyList()
        } else {
            view?.showPasswords(filtered)
        }
    }

    override fun refreshClick() {
        view?.showProgress()
        fetchPasswords()
    }

    override fun refreshSwipe() {
        fetchPasswords()
    }

    override fun moreClick(passwordModel: PasswordModel) {
        view?.navigateToMore(passwordModel)
    }

    override fun itemClick() {
        view?.navigateToDetails()
    }
}
