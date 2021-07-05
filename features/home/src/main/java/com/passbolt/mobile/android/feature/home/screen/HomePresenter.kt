package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.ui.PasswordModel
import com.passbolt.mobile.android.feature.home.screen.usecase.GetResourcesUseCase
import com.passbolt.mobile.android.mappers.ResourceModelMapper
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
class HomePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val resourceModelMapper: ResourceModelMapper
) : HomeContract.Presenter {

    override var view: HomeContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: HomeContract.View) {
        super.attach(view)
        fetchPasswords()
    }

    private fun fetchPasswords() {
        scope.launch {
            when (val result = getResourcesUseCase.execute(Unit)) {
                GetResourcesUseCase.Output.Failure -> {
                    view?.hideRefreshProgress()
                    view?.hideProgress()
                    view?.showError()
                }
                is GetResourcesUseCase.Output.Success -> {
                    view?.hideRefreshProgress()
                    view?.hideProgress()
                    val resources = result.resources.map { resourceModelMapper.map(it) }
                    view?.showPasswords(resources)
                }
            }
        }
    }

    override fun refreshClick() {
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
