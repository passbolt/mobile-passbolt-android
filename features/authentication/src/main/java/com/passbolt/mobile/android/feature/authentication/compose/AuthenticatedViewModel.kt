package com.passbolt.mobile.android.feature.authentication.compose

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.authentication.MfaProvidersHandler
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.AuthenticationRefreshed
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.OtherProviderClick
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowDuoDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowTotpDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowUnknownProvider
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowYubikeyDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
open class AuthenticatedViewModel<ViewState, SideEffect>(
    initialState: ViewState,
) : SideEffectViewModel<ViewState, SideEffect>(initialState),
    KoinComponent {
    private val authenticationSideEffectChannel = Channel<AuthenticationSideEffect>()
    val authenticationSideEffect: Flow<AuthenticationSideEffect> = authenticationSideEffectChannel.receiveAsFlow()

    private val sessionRefreshTrackingFlow: SessionRefreshTrackingFlow by inject()

    private val mfaProvidersHandler: MfaProvidersHandler by inject()
    private val getSessionUseCase: GetSessionUseCase by inject()

    init {
        listenForRefreshSessionEvents()
    }

    private fun listenForRefreshSessionEvents() {
        Timber.d("[Session] Listening for new session events in ${this::class.simpleName}")
        viewModelScope.launch {
            sessionRefreshTrackingFlow
                .needSessionRefreshFlow()
                .take(1)
                .collect {
                    Timber.d("[Session] MVI Session refresh is needed: [${it.reason}] in ${this@AuthenticatedViewModel}. Showing UI.")
                    when (val reason = it.reason) {
                        is Mfa -> {
                            mfaProvidersHandler.setProviders(reason.providers.orEmpty())
                            emitSideAuthenticationEffect(
                                AuthenticationSideEffect.ShowMfaAuth(
                                    mfaReason = mfaProvidersHandler.firstMfaProvider(),
                                    hasMultipleProviders = mfaProvidersHandler.hasMultipleProviders(),
                                    sessionAccessToken = getSessionUseCase.execute(Unit).accessToken,
                                ),
                            )
                        }
                        is Passphrase -> {
                            emitSideAuthenticationEffect(
                                AuthenticationSideEffect.ShowAuth(ActivityIntents.AuthConfig.RefreshPassphrase),
                            )
                        }
                        is Session -> {
                            emitSideAuthenticationEffect(AuthenticationSideEffect.ShowAuth(ActivityIntents.AuthConfig.SignIn))
                        }
                    }
                }
        }
    }

    fun onAuthenticationIntent(intent: AuthenticatedIntent) {
        Timber.d("[Session] Received authentication intent [${intent::class.simpleName}]")
        when (intent) {
            AuthenticationRefreshed -> authenticationRefreshed()
            is OtherProviderClick -> otherProviderClick(intent.provider)
        }
    }

    private fun otherProviderClick(currentProvider: MfaProvider) {
        val sessionAccessToken = getSessionUseCase.execute(Unit).accessToken
        when (mfaProvidersHandler.nextMfaProvider(currentProvider)) {
            YUBIKEY ->
                emitSideAuthenticationEffect(
                    ShowYubikeyDialog(
                        hasOtherProviders = mfaProvidersHandler.hasMultipleProviders(),
                        sessionAccessToken = sessionAccessToken,
                    ),
                )
            TOTP ->
                emitSideAuthenticationEffect(
                    ShowTotpDialog(
                        hasOtherProviders = mfaProvidersHandler.hasMultipleProviders(),
                        sessionAccessToken = sessionAccessToken,
                    ),
                )
            DUO ->
                emitSideAuthenticationEffect(
                    ShowDuoDialog(
                        hasOtherProviders = mfaProvidersHandler.hasMultipleProviders(),
                        sessionAccessToken = sessionAccessToken,
                    ),
                )
            null ->
                emitSideAuthenticationEffect(
                    ShowUnknownProvider(hasOtherProviders = mfaProvidersHandler.hasMultipleProviders()),
                )
        }
    }

    private fun authenticationRefreshed() {
        sessionRefreshTrackingFlow.notifySessionRefreshed()
    }

    private fun emitSideAuthenticationEffect(event: AuthenticationSideEffect) {
        launch {
            authenticationSideEffectChannel.send(event)
        }
    }
}
