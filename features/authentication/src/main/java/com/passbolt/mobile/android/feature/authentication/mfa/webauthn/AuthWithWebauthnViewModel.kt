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
import com.passbolt.mobile.android.domain.mfa.usecase.VerifyWebauthnDelegatedTokenUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyWebauthnDelegatedTokenLocallyUseCase
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
    private val userId: String,
    private val authToken: String?,
    private val getWebauthnDelegatedPromptUseCase: GetWebauthnDelegatedPromptUseCase,
    private val getPasskeyCeremonyUrlUseCase: GetPasskeyCeremonyUrlUseCase,
    private val verifyWebauthnDelegatedTokenLocallyUseCase: VerifyWebauthnDelegatedTokenLocallyUseCase,
    private val verifyWebauthnDelegatedTokenUseCase: VerifyWebauthnDelegatedTokenUseCase,
    private val signOutUseCase: SignOutUseCase,
) : SideEffectViewModel<AuthWithWebauthnState, AuthWithWebauthnSideEffect>(
        AuthWithWebauthnState(hasOtherProvider = hasOtherProvider),
    ) {
    private var ceremony: DelegatedCeremony? = null

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
            Timber.d("Requesting webauthn delegated state")
            val result = getWebauthnDelegatedPromptUseCase.execute(GetWebauthnDelegatedPromptUseCase.Input(token))
            when (result) {
                is GetWebauthnDelegatedPromptUseCase.Output.Success -> {
                    ceremony = DelegatedCeremony(state = result.state, secret = result.secret)
                    Timber.d("Launching webauthn ceremony in browser")
                    val ceremonyUrl =
                        getPasskeyCeremonyUrlUseCase
                            .execute(GetPasskeyCeremonyUrlUseCase.Input(result.state))
                            .url
                    emitSideEffect(LaunchWebauthnCeremony(ceremonyUrl))
                }
                is GetWebauthnDelegatedPromptUseCase.Output.Unauthorized -> sessionExpired()
                is GetWebauthnDelegatedPromptUseCase.Output.NetworkFailure -> {
                    Timber.e("Webauthn delegated state request failed with code ${result.errorCode}")
                    emitSideEffect(
                        ShowErrorSnackbar(
                            if (result.errorCode == HTTP_BAD_REQUEST) PROVIDER_UNAVAILABLE else GENERIC,
                        ),
                    )
                }
                is GetWebauthnDelegatedPromptUseCase.Output.Failure -> {
                    Timber.e("Webauthn delegated state request did not complete")
                    emitSideEffect(ShowErrorSnackbar(result.incomplete.toSnackbarErrorType()))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun ceremonyFinished(result: WebauthnCeremonyResult) {
        when (result) {
            is Completed -> completeCeremony(ceremony, result.delegatedToken)
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
        ceremony = null
    }

    private fun completeCeremony(
        pendingCeremony: DelegatedCeremony?,
        delegatedToken: String,
    ) {
        val token = authToken
        if (pendingCeremony == null) {
            Timber.e("Webauthn ceremony completed without a pending delegated state")
            emitSideEffect(ShowErrorSnackbar(CEREMONY_FAILED))
            return
        }
        if (token == null) {
            Timber.e("Authentication token is null, cannot exchange webauthn delegated token")
            emitSideEffect(ShowErrorSnackbar(GENERIC))
            return
        }
        updateViewState { copy(showProgress = true) }
        launch {
            val localCheck =
                verifyWebauthnDelegatedTokenLocallyUseCase.execute(
                    VerifyWebauthnDelegatedTokenLocallyUseCase.Input(
                        userId = userId,
                        delegatedToken = delegatedToken,
                        expectedState = pendingCeremony.state,
                    ),
                )
            when (localCheck) {
                is VerifyWebauthnDelegatedTokenLocallyUseCase.Output.Valid -> {
                    Timber.d("Webauthn delegated token verified locally, exchanging it for MFA token")
                    exchangeDelegatedToken(token, delegatedToken, pendingCeremony.secret)
                }
                is VerifyWebauthnDelegatedTokenLocallyUseCase.Output.Invalid -> {
                    Timber.e("Webauthn delegated token rejected locally: ${localCheck.reason}")
                    emitSideEffect(ShowErrorSnackbar(CEREMONY_FAILED))
                }
                is VerifyWebauthnDelegatedTokenLocallyUseCase.Output.ServerKeyUnavailable -> {
                    Timber.e("Server public key is not available, cannot verify webauthn delegated token")
                    emitSideEffect(ShowErrorSnackbar(GENERIC))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun exchangeDelegatedToken(
        token: String,
        delegatedToken: String,
        delegatedSecret: String,
    ) {
        val result =
            verifyWebauthnDelegatedTokenUseCase.execute(
                VerifyWebauthnDelegatedTokenUseCase.Input(
                    jwtHeader = token,
                    delegatedToken = delegatedToken,
                    delegatedSecret = delegatedSecret,
                    remember = viewState.value.rememberMe,
                ),
            )
        when (result) {
            is VerifyWebauthnDelegatedTokenUseCase.Output.Success -> verificationSuccess(result.mfaHeader)
            is VerifyWebauthnDelegatedTokenUseCase.Output.Unauthorized -> sessionExpired()
            is VerifyWebauthnDelegatedTokenUseCase.Output.NetworkFailure -> {
                Timber.e("Webauthn delegated token exchange failed with code ${result.errorCode}")
                emitSideEffect(
                    ShowErrorSnackbar(
                        if (result.errorCode == HTTP_BAD_REQUEST) VERIFICATION_EXPIRED else GENERIC,
                    ),
                )
            }
            is VerifyWebauthnDelegatedTokenUseCase.Output.Failure -> {
                Timber.e("Webauthn delegated token exchange did not complete")
                emitSideEffect(ShowErrorSnackbar(result.incomplete.toSnackbarErrorType()))
            }
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

private data class DelegatedCeremony(
    val state: String,
    val secret: String,
)
