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

package com.passbolt.mobile.android.core.mvp.authentication

import androidx.annotation.CallSuper
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

// TODO consider updating to support multiple async launches as now there would be a use case for that:
// TODO fetching resources and folders on start in parallel
abstract class BaseAuthenticatedPresenter<T : BaseAuthenticatedContract.View>(
    coroutineLaunchContext: CoroutineLaunchContext,
) : BaseAuthenticatedContract.Presenter<T>,
    KoinComponent {
    private val sessionRefreshTrackingFlow: SessionRefreshTrackingFlow by inject()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private val mfaProvidersHandler: MfaProvidersHandler by inject()

    @CallSuper
    override fun attach(view: T) {
        super.attach(view)
        Timber.d("[Session] Attaching base networking presenter for $view")
        listenForRefreshSessionEvents()
    }

    private fun listenForRefreshSessionEvents() {
        Timber.d("[Session] Listening for new session events")
        scope.launch {
            sessionRefreshTrackingFlow
                .needSessionRefreshFlow()
                .collect {
                    Timber.d("[Session] MVP Session refresh is needed: [${it.reason}] in ${this@BaseAuthenticatedPresenter}. Showing UI.")
                    when (val reason = it.reason) {
                        is Mfa -> {
                            mfaProvidersHandler.setProviders(reason.providers.orEmpty())
                            view?.showMfaAuth(
                                mfaProvidersHandler.firstMfaProvider(),
                                mfaProvidersHandler.hasMultipleProviders(),
                            )
                        }
                        is Passphrase -> {
                            view?.showRefreshPassphraseAuth()
                        }
                        is Session -> {
                            view?.showSignInAuth()
                        }
                    }
                }
        }
    }

    @CallSuper
    override fun detach() {
        Timber.d("[Session] Detaching base networking presenter for $view")
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    override fun authenticationRefreshed() {
        sessionRefreshTrackingFlow.notifySessionRefreshed()
    }

    override fun otherProviderClick(currentProvider: MfaProvider) {
        when (mfaProvidersHandler.nextMfaProvider(currentProvider)) {
            YUBIKEY -> view?.showYubikeyDialog(mfaProvidersHandler.hasMultipleProviders())
            TOTP -> view?.showTotpDialog(mfaProvidersHandler.hasMultipleProviders())
            DUO -> view?.showDuoDialog(mfaProvidersHandler.hasMultipleProviders())
            null -> view?.showUnknownProvider()
        }
    }
}
