package com.passbolt.mobile.android.feature.authentication.session

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.navigation.AppForegroundListener
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

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

private const val SESSION_DURATION_BEFORE_SKEW_SECONDS = 30L

/**
 * Runs operation which requires authentication (backed or local passphrase). If operation has unauthenticated status
 * it is communicated to the caller (@see BaseAuthenticatedContract) and awaiting on authentication refresh starts. After receiving authentication
 * refreshed event the initial operation is automatically restarted.
 *
 */
class AuthenticatedOperationRunner : KoinComponent {
    private val refreshSessionUseCase: RefreshSessionUseCase by inject()
    private val getSessionExpiryUseCase: GetSessionExpiryUseCase by inject()
    private val passphraseMemoryCache: PassphraseMemoryCache by inject()
    private val appForegroundListener: AppForegroundListener by inject()
    private val sessionRefreshTrackingFlow: SessionRefreshTrackingFlow by inject()

    suspend fun <OUTPUT : AuthenticatedUseCaseOutput> runOperation(request: suspend () -> OUTPUT): OUTPUT {
        val needFullSignIn = isFullSignInNeeded()
        val needPassphraseRefresh = isPassphraseRefreshNeeded()

        // session is refreshed proactively to avoid waiting for the first request to fail
        // bot local and backend sessions are checked
        refreshSessionProactively(needFullSignIn, needPassphraseRefresh)

        Timber.d("[Session] Running operation")
        val response = request.invoke()
        val authenticationState = response.authenticationState
        return if (authenticationState is Unauthenticated) {
            // sometimes even with proactive refresh we may receive Unauthenticated state from backend
            // i.e. after server key rotation for all the users
            Timber.d("[Session] Operation is unauthenticated, starting UI authentication")
            authenticateUsingSignInUi(authenticationState.reason)
            runOperation(request)
        } else {
            response
        }
    }

    private suspend fun refreshSessionProactively(
        needFullSignIn: Boolean,
        needPassphraseRefresh: Boolean,
    ) {
        val sessionRefreshReason =
            if (needFullSignIn) {
                Reason.Session
            } else if (needPassphraseRefresh) {
                Reason.Passphrase
            } else {
                null
            }

        sessionRefreshReason?.let {
            authenticateUsingSignInUi(it)
        }
    }

    private fun isPassphraseRefreshNeeded(): Boolean {
        val sessionDurationSeconds = passphraseMemoryCache.getSessionDurationSeconds()
        return if (sessionDurationSeconds == null || sessionDurationSeconds < SESSION_DURATION_BEFORE_SKEW_SECONDS) {
            Timber.d("[Session] Passphrase session is not valid for request")
            true
        } else {
            Timber.d("[Session] Passphrase session is valid for request")
            false
        }
    }

    private suspend fun isFullSignInNeeded() =
        when (val sessionExpiry = getSessionExpiryUseCase.execute(Unit)) {
            is GetSessionExpiryUseCase.Output.NoJwt -> {
                Timber.d("[Session] Access token expiry is null")
                true
            }
            is GetSessionExpiryUseCase.Output.JwtAlreadyExpired -> {
                Timber.d("[Session] Session is expired, refreshing in background")
                !backgroundRefreshSessionSessionSucceeded()
            }
            is GetSessionExpiryUseCase.Output.JwtWillExpire -> {
                if (!sessionExpiry.accessTokenExpirySeconds.isAfter(
                        ZonedDateTime.now().plusSeconds(SESSION_DURATION_BEFORE_SKEW_SECONDS),
                    )
                ) {
                    Timber.d("[Session] Session may end before finishing current request, refreshing in background")
                    !backgroundRefreshSessionSessionSucceeded()
                } else {
                    Timber.d("[Session] Session is valid for request")
                    false
                }
            }
        }

    private suspend fun backgroundRefreshSessionSessionSucceeded(): Boolean {
        Timber.d("[Session] Starting background session refresh")
        return when (refreshSessionUseCase.execute(Unit)) {
            is RefreshSessionUseCase.Output.Success -> {
                Timber.d("[Session] Background session refresh succeeded")
                true
            }
            is RefreshSessionUseCase.Output.Failure -> {
                Timber.d("[Session] Background session refresh did not succeed - launching sign in")
                false
            }
        }
    }

    private suspend fun authenticateUsingSignInUi(reason: UnauthenticatedReason) {
        if (!appForegroundListener.isForeground()) {
            Timber.d("[Session] App is in background, waiting for foreground to start UI authentication")
            appForegroundListener.appWentForegroundFlow.take(1).collect()
            Timber.d("[Session] App is in foreground")
        }

        Timber.d("[Session] Starting UI authentication")
        sessionRefreshTrackingFlow.notifySessionRefreshNeeded(reason)
        sessionRefreshTrackingFlow
            .sessionRefreshedFlow()
            .first()
        Timber.d("[Session] Authenticated operation runner $this got refreshed auth")
    }
}

/**
 * Runs an operation which requires authentication using AuthenticatedOperationRunner
 * @see AuthenticatedOperationRunner
 */
suspend fun <OUTPUT : AuthenticatedUseCaseOutput> runAuthenticatedOperation(request: suspend () -> OUTPUT): OUTPUT =
    AuthenticatedOperationRunner().runOperation(request)
