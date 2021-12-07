package com.passbolt.mobile.android.core.mvp.authentication

import androidx.annotation.CallSuper
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
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
abstract class BaseAuthenticatedPresenter<T : BaseAuthenticatedContract.View>(
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedContract.Presenter<T> {

    private var _sessionRefreshFlow = MutableStateFlow<Unit?>(null)
    protected val sessionRefreshedFlow
        get() = _sessionRefreshFlow.asStateFlow()

    protected lateinit var needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    @CallSuper
    override fun attach(view: T) {
        super.attach(view)
        Timber.d("[Session] Attaching base networking presenter for $view")
        listenForRefreshSessionEvents()
    }

    private fun listenForRefreshSessionEvents() {
        Timber.d("[Session] Listening for new session evetns")
        needSessionRefreshFlow = MutableStateFlow(null)
        scope.launch {
            needSessionRefreshFlow
                .drop(1) // drop initial value
                .take(1)
                .collect {
                    Timber.d("[Session] Session refresh needed - showing auth")
                    it?.let { view?.showAuth(it) }
                    listenForRefreshSessionEvents()
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
        _sessionRefreshFlow.value = Unit
        _sessionRefreshFlow = MutableStateFlow(null)
    }
}
