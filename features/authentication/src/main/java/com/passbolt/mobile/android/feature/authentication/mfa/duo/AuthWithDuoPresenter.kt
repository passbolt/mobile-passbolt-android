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

package com.passbolt.mobile.android.feature.authentication.mfa.duo

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyDuoCallbackUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet.DuoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthWithDuoPresenter(
    private val getDuoPromptUseCase: GetDuoPromptUseCase,
    private val verifyDuoCallbackUseCase: VerifyDuoCallbackUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val signOutUseCase: SignOutUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthWithDuoContract.Presenter {

    override var view: AuthWithDuoContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private var authToken: String? = null
    private var passboltDuoCookieUuid: String? = null

    override fun onViewCreated(hasOtherProvider: Boolean, authToken: String?) {
        this.authToken = authToken
        view?.showChangeProviderButton(hasOtherProvider)
    }

    override fun authWithDuoClick() {
        view?.showProgress()
        scope.launch {
            authToken?.let {
                when (val duoPromptResult =
                    getDuoPromptUseCase.execute(GetDuoPromptUseCase.Input(it, remember = false))) {
                    is GetDuoPromptUseCase.Output.DuoPromptUrlNotFound -> view?.showError()
                    is GetDuoPromptUseCase.Output.Failure<*> -> view?.showError()
                    is GetDuoPromptUseCase.Output.NetworkFailure -> view?.showError()
                    is GetDuoPromptUseCase.Output.Unauthorized -> if (backgroundSessionRefreshSucceeded()) {
                        authWithDuoClick() // restart operation after background session refresh
                    } else {
                        view?.run {
                            navigateToLogin()
                        }
                    }
                    is GetDuoPromptUseCase.Output.Success -> {
                        passboltDuoCookieUuid = duoPromptResult.passboltDuoCookieUuid
                        view?.navigateToDuoPrompt(duoPromptResult.duoPromptUrl)
                    }
                }
            }
            view?.hideProgress()
        }
    }

    override fun verifyDuoAuth(state: DuoState) {
        view?.showProgress()
        scope.launch {
            val (authToken, duoCookie) = authToken to passboltDuoCookieUuid
            if (authToken != null && duoCookie != null) {
                when (val duoVerificationResult =
                    verifyDuoCallbackUseCase.execute(
                        VerifyDuoCallbackUseCase.Input(
                            jwtHeader = authToken,
                            passboltDuoCookieUuid = duoCookie,
                            duoState = state.state,
                            duoCode = state.duoCode
                        )
                    )) {
                    is VerifyDuoCallbackUseCase.Output.Error -> view?.showError()
                    is VerifyDuoCallbackUseCase.Output.Failure<*> -> view?.showError()
                    is VerifyDuoCallbackUseCase.Output.Unauthorized -> if (backgroundSessionRefreshSucceeded()) {
                        verifyDuoAuth(state) // restart operation after background session refresh
                    } else {
                        view?.run {
                            navigateToLogin()
                        }
                    }
                    is VerifyDuoCallbackUseCase.Output.Success -> {
                        duoSuccess(duoVerificationResult.mfaHeader)
                    }
                }
            } else {
                Timber.e("Authentication token or duo uuid cookie is null")
            }
            view?.hideProgress()
        }
    }

    private fun duoSuccess(mfaHeader: String?) {
        mfaHeader?.let {
            view?.notifyVerificationSucceeded(it)
        } ?: run {
            view?.showError()
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() =
        refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    override fun authenticationSucceeded() {
        view?.apply {
            close()
            notifyLoginSucceeded()
        }
    }

    override fun closeClick() {
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.closeAndNavigateToStartup()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
