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

package com.passbolt.mobile.android.feature.settings.screen

import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsPresenter(
    private val signOutUseCase: SignOutUseCase,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
    coroutineLaunchContext: CoroutineLaunchContext
) : SettingsContract.Presenter {

    override var view: SettingsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun signOutClick() {
        scope.launch {
            view?.showProgress()
            fullDataRefreshExecutor.dataRefreshStatusFlow.first { it is DataRefreshStatus.Finished }
            view?.hideProgress()
            view?.showLogoutDialog()
        }
    }

    override fun logoutConfirmed() {
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.navigateToSignInWithLogout()
        }
    }

    override fun termsAndLicensesClick() {
        view?.navigateToTermsAndLicensesSettings()
    }

    override fun debugLogsSettingsClick() {
        view?.navigateToDebugLogsSettings()
    }

    override fun accountsSettingsClick() {
        view?.navigateAccountsSettings()
    }

    override fun appSettingsClick() {
        view?.navigateToAppSettings()
    }
}
