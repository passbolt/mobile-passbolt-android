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

package com.passbolt.mobile.android.feature.authentication.compose

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.compose.RepeatOnStartedEffect
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.authentication.MfaProvidersHandler
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.RefreshSession
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoScreen
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpScreen
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderScreen
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyScreen
import org.koin.compose.koinInject

/**
 * Activity-level composable that handles authentication refresh for all screens within an activity.
 * Replaces the per-screen AuthenticationHandler pattern by collecting SessionRefreshTrackingFlow
 * directly and launching authentication UI when needed.
 *
 * Place this once in each activity's root composable (MainScreen, AutofillResourcesScreen).
 */
@Composable
fun AuthenticationHandler(
    sessionRefreshTrackingFlow: SessionRefreshTrackingFlow = koinInject(),
    mfaProvidersHandler: MfaProvidersHandler = koinInject(),
    getSessionUseCase: GetSessionUseCase = koinInject(),
) {
    val context = LocalContext.current

    var mfaDialogState by remember { mutableStateOf<MfaDialogState?>(null) }

    val authenticationResult =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                sessionRefreshTrackingFlow.notifySessionRefreshed()
            }
        }

    RepeatOnStartedEffect {
        sessionRefreshTrackingFlow.needSessionRefreshFlow().collect { sessionState ->
            when (val reason = sessionState.reason) {
                is Mfa -> {
                    mfaProvidersHandler.setProviders(reason.providers.orEmpty())
                    mfaDialogState =
                        toMfaDialogState(
                            mfaReason = mfaProvidersHandler.firstMfaProvider(),
                            hasMultipleProviders = mfaProvidersHandler.hasMultipleProviders(),
                            sessionAccessToken = getSessionUseCase.execute(Unit).accessToken,
                        )
                }
                is Passphrase -> {
                    authenticationResult.launch(
                        ActivityIntents.authentication(context, RefreshPassphrase),
                    )
                }
                is Session -> {
                    authenticationResult.launch(
                        ActivityIntents.authentication(context, RefreshSession),
                    )
                }
            }
        }
    }

    mfaDialogState?.let { state ->
        MfaDialog(
            state = state,
            onMfaResult = { result ->
                mfaDialogState = null
                when (result) {
                    is MfaResult.Succeeded -> sessionRefreshTrackingFlow.notifySessionRefreshed()
                    is MfaResult.OtherProvider -> {
                        mfaDialogState =
                            toMfaDialogState(
                                mfaReason = mfaProvidersHandler.nextMfaProvider(result.currentProvider),
                                hasMultipleProviders = mfaProvidersHandler.hasMultipleProviders(),
                                sessionAccessToken = getSessionUseCase.execute(Unit).accessToken,
                            )
                    }
                }
            },
        )
    }
}

@Composable
private fun MfaDialog(
    state: MfaDialogState,
    onMfaResult: (MfaResult) -> Unit,
) {
    when (state) {
        is MfaDialogState.Totp -> EnterTotpScreen(mfaState = state, onMfaResult = onMfaResult)
        is MfaDialogState.Yubikey -> ScanYubikeyScreen(mfaState = state, onMfaResult = onMfaResult)
        is MfaDialogState.Duo -> AuthWithDuoScreen(mfaState = state, onMfaResult = onMfaResult)
        is MfaDialogState.UnknownProvider -> UnknownProviderScreen()
    }
}

private fun toMfaDialogState(
    mfaReason: MfaProvider?,
    hasMultipleProviders: Boolean,
    sessionAccessToken: String?,
): MfaDialogState =
    when (mfaReason) {
        MfaProvider.TOTP -> MfaDialogState.Totp(authToken = sessionAccessToken, hasOtherProviders = hasMultipleProviders)
        MfaProvider.YUBIKEY -> MfaDialogState.Yubikey(authToken = sessionAccessToken, hasOtherProviders = hasMultipleProviders)
        MfaProvider.DUO -> MfaDialogState.Duo(authToken = sessionAccessToken, hasOtherProviders = hasMultipleProviders)
        null -> MfaDialogState.UnknownProvider
    }
