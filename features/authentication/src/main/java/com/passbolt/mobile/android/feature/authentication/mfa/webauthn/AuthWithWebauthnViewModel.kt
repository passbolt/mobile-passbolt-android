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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.domain.mfa.usecase.GetPasskeyCeremonyUrlUseCase
import com.passbolt.mobile.android.domain.mfa.usecase.GetWebauthnDelegatedPromptUseCase
import com.passbolt.mobile.android.domain.mfa.usecase.VerifyWebauthnExchangeCodeUseCase
import com.passbolt.mobile.android.feature.authentication.auth.OneTimeSessionRefreshRetry
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.AuthenticateWithWebauthn
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.WebauthnCeremonyFinished
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.LaunchWebauthnCeremony
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyOtherProviderClicked
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.BROWSER_UNAVAILABLE
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.CEREMONY_FAILED
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.NETWORK
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.PROVIDER_UNAVAILABLE
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.SESSION_EXPIRED
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.VERIFICATION_EXPIRED
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.BrowserUnavailable
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Completed
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed
import timber.log.Timber
import java.net.HttpURLConnection.HTTP_BAD_REQUEST

class AuthWithWebauthnViewModel(
    hasOtherProvider: Boolean,
    private val isSetupFlow: Boolean,
    private val authToken: String?,
    private val getWebauthnDelegatedPromptUseCase: GetWebauthnDelegatedPromptUseCase,
    private val verifyWebauthnExchangeCodeUseCase: VerifyWebauthnExchangeCodeUseCase,
    private val getPasskeyCeremonyUrlUseCase: GetPasskeyCeremonyUrlUseCase,
    private val oneTimeSessionRefreshRetry: OneTimeSessionRefreshRetry,
    private val signOutUseCase: SignOutUseCase,
) : SideEffectViewModel<AuthWithWebauthnState, AuthWithWebauthnSideEffect>(
        AuthWithWebauthnState(hasOtherProvider = hasOtherProvider),
    ) {
    fun onIntent(intent: AuthWithWebauthnIntent) {
        when (intent) {
            is AuthenticateWithWebauthn -> authenticateWithWebauthn()
            is WebauthnCeremonyFinished -> ceremonyFinished(intent.result)
            is ToggleRememberMe -> updateViewState { copy(rememberMe = intent.checked) }
            is ChooseOtherProvider -> emitSideEffect(NotifyOtherProviderClicked(authToken))
            is Close -> close()
            is ConfirmSetupLeave -> {
                updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
                signOutAndClose()
            }
            is DismissSetupLeave -> updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
        }
    }

    private fun close() {
        if (isSetupFlow) {
            updateViewState { copy(showSetupLeaveConfirmationDialog = true) }
        } else {
            signOutAndClose()
        }
    }

    private fun authenticateWithWebauthn() {
        val token = authToken
        if (token == null) {
            Timber.e("Authentication token is null, cannot start webauthn ceremony")
            emitSideEffect(ShowErrorSnackbar(GENERIC))
            return
        }
        updateViewState { copy(showProgress = true) }
        launch {
            Timber.d("Requesting webauthn ceremony token")
            val result =
                oneTimeSessionRefreshRetry.execute(
                    token = token,
                    isUnauthorized = { it is GetWebauthnDelegatedPromptUseCase.Output.Unauthorized },
                ) { bearer ->
                    getWebauthnDelegatedPromptUseCase.execute(GetWebauthnDelegatedPromptUseCase.Input(bearer))
                }
            when (result) {
                is GetWebauthnDelegatedPromptUseCase.Output.Success -> {
                    Timber.d("Launching webauthn ceremony in browser")
                    val ceremonyUrl =
                        getPasskeyCeremonyUrlUseCase
                            .execute(GetPasskeyCeremonyUrlUseCase.Input(result.ceremonyToken))
                            .url
                    emitSideEffect(LaunchWebauthnCeremony(ceremonyUrl))
                }
                is GetWebauthnDelegatedPromptUseCase.Output.Unauthorized -> sessionExpired()
                is GetWebauthnDelegatedPromptUseCase.Output.NetworkFailure -> {
                    Timber.e("Webauthn ceremony token request failed with code ${result.errorCode}")
                    emitSideEffect(
                        ShowErrorSnackbar(
                            if (result.errorCode == HTTP_BAD_REQUEST) PROVIDER_UNAVAILABLE else GENERIC,
                        ),
                    )
                }
                is GetWebauthnDelegatedPromptUseCase.Output.Failure -> {
                    Timber.e("Webauthn ceremony token request did not complete")
                    emitSideEffect(ShowErrorSnackbar(result.incomplete.toSnackbarErrorType()))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun ceremonyFinished(result: WebauthnCeremonyResult) {
        when (result) {
            is Completed -> {
                Timber.d("Webauthn ceremony completed, exchanging code for MFA token")
                verifyExchangeCode(result.exchangeCode)
            }
            is Cancelled -> Timber.d("Webauthn ceremony cancelled")
            is BrowserUnavailable -> {
                Timber.e("No browser available to run the webauthn ceremony")
                emitSideEffect(ShowErrorSnackbar(BROWSER_UNAVAILABLE))
            }
            is Failed -> {
                Timber.e("Webauthn ceremony failed: ${result.error}")
                emitSideEffect(ShowErrorSnackbar(CEREMONY_FAILED))
            }
        }
    }

    private fun verifyExchangeCode(exchangeCode: String) {
        val token = authToken
        if (token == null) {
            Timber.e("Authentication token is null, cannot exchange webauthn code")
            emitSideEffect(ShowErrorSnackbar(GENERIC))
            return
        }
        updateViewState { copy(showProgress = true) }
        launch {
            val result =
                oneTimeSessionRefreshRetry.execute(
                    token = token,
                    isUnauthorized = { it is VerifyWebauthnExchangeCodeUseCase.Output.Unauthorized },
                ) { bearer ->
                    verifyWebauthnExchangeCodeUseCase.execute(
                        VerifyWebauthnExchangeCodeUseCase.Input(
                            jwtHeader = bearer,
                            exchangeCode = exchangeCode,
                            remember = viewState.value.rememberMe,
                        ),
                    )
                }
            when (result) {
                is VerifyWebauthnExchangeCodeUseCase.Output.Success -> verificationSuccess(result.mfaHeader)
                is VerifyWebauthnExchangeCodeUseCase.Output.Unauthorized -> sessionExpired()
                is VerifyWebauthnExchangeCodeUseCase.Output.NetworkFailure -> {
                    Timber.e("Webauthn exchange code verification failed with code ${result.errorCode}")
                    emitSideEffect(
                        ShowErrorSnackbar(
                            if (result.errorCode == HTTP_BAD_REQUEST) VERIFICATION_EXPIRED else GENERIC,
                        ),
                    )
                }
                is VerifyWebauthnExchangeCodeUseCase.Output.Failure -> {
                    Timber.e("Webauthn exchange code verification did not complete")
                    emitSideEffect(ShowErrorSnackbar(result.incomplete.toSnackbarErrorType()))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun verificationSuccess(mfaHeader: String?) {
        if (mfaHeader != null) {
            Timber.d("Webauthn MFA verification succeeded")
            emitSideEffect(NotifyVerificationSucceeded(mfaHeader))
        } else {
            Timber.e("Webauthn MFA verification succeeded but MFA cookie is missing")
            emitSideEffect(ShowErrorSnackbar(GENERIC))
        }
    }

    private fun sessionExpired() {
        Timber.d("Session expired during webauthn verification")
        emitSideEffect(ShowErrorSnackbar(SESSION_EXPIRED))
        emitSideEffect(NavigateToLogin)
    }

    private fun DomainResult.Incomplete.toSnackbarErrorType(): SnackbarErrorType =
        when (this) {
            is DomainResult.Incomplete.Error ->
                when (reason) {
                    DomainResult.Incomplete.Error.Reason.OFFLINE,
                    DomainResult.Incomplete.Error.Reason.TIMEOUT,
                    -> NETWORK
                    DomainResult.Incomplete.Error.Reason.SERVER,
                    DomainResult.Incomplete.Error.Reason.UNKNOWN,
                    -> GENERIC
                }
            else -> GENERIC
        }

    private fun signOutAndClose() {
        launch {
            updateViewState { copy(showProgress = true) }
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(CloseAndNavigateToStartup)
        }
    }
}
