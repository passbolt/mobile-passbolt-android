package com.passbolt.mobile.android.core.networking.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
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
class SessionAwareRequestRunner(
    private val needSessionRefreshedFlow: MutableStateFlow<Unit?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>
) {

    suspend fun <OUTPUT : NetworkingUseCaseOutput> runRequest(
        request: suspend () -> OUTPUT
    ): OUTPUT {
        val response = request.invoke()
        return if (!response.isUnauthorized) {
            response
        } else {
            Timber.d("[Session] Request runner $this waits for session refresh")
            needSessionRefreshedFlow.tryEmit(Unit)
            sessionRefreshedFlow
                .drop(1) // drop initial value
                .take(1) // wait for first session refreshed item
                .collect {
                    Timber.d("[Session] Request runner $this got refreshed session")
                }
            Timber.d("[Session] Request runner $this restarts initial request")
            request.invoke()
        }
    }
}

suspend fun <OUTPUT : NetworkingUseCaseOutput> runRequest(
    needSessionRefresh: MutableStateFlow<Unit?>,
    sessionRefreshedFlow: StateFlow<Unit?>,
    request: suspend () -> OUTPUT
): OUTPUT =
    SessionAwareRequestRunner(needSessionRefresh, sessionRefreshedFlow).runRequest(request)
