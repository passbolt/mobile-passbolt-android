package com.passbolt.mobile.android.feature.authentication.mfa.duo

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase.Output.DuoPromptUrlNotFound
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase.Output.Failure
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase.Output.NetworkFailure
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase.Output.Success
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase.Output.Unauthorized
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyDuoCallbackUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.AuthenticateWithDuo
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DismissDuoAuth
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DuoAuthFinished
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NotifyOtherProviderClicked
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet.DuoState
import timber.log.Timber

class AuthWithDuoViewModel(
    hasOtherProvider: Boolean,
    private val authToken: String?,
    private val getDuoPromptUseCase: GetDuoPromptUseCase,
    private val verifyDuoCallbackUseCase: VerifyDuoCallbackUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val signOutUseCase: SignOutUseCase,
) : SideEffectViewModel<AuthWithDuoState, AuthWithDuoSideEffect>(
        AuthWithDuoState(hasOtherProvider = hasOtherProvider),
    ) {
    private var passboltDuoCookieUuid: String? = null

    fun onIntent(intent: AuthWithDuoIntent) {
        when (intent) {
            is AuthenticateWithDuo -> authenticateWithDuo()
            is ChooseOtherProvider -> emitSideEffect(NotifyOtherProviderClicked(authToken))
            is DismissDuoAuth -> updateViewState { copy(showDuoWebViewSheet = false) }
            is Close -> signOutAndClose()
            is DuoAuthFinished -> verifyDuoAuth(intent.state)
        }
    }

    private fun authenticateWithDuo() {
        updateViewState { copy(showProgress = true) }
        launch {
            authToken?.let { token ->
                when (val result = getDuoPromptUseCase.execute(GetDuoPromptUseCase.Input(token))) {
                    is DuoPromptUrlNotFound ->
                        emitSideEffect(ShowErrorSnackbar(GENERIC))
                    is Failure<*> ->
                        emitSideEffect(ShowErrorSnackbar(GENERIC))
                    is NetworkFailure ->
                        emitSideEffect(ShowErrorSnackbar(GENERIC))
                    is Unauthorized -> {
                        if (backgroundSessionRefreshSucceeded()) {
                            authenticateWithDuo()
                            return@launch
                        } else {
                            emitSideEffect(NavigateToLogin)
                        }
                    }
                    is Success -> {
                        passboltDuoCookieUuid = result.passboltDuoCookieUuid
                        updateViewState { copy(showDuoWebViewSheet = true, duoPromptUrl = result.duoPromptUrl) }
                    }
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun verifyDuoAuth(duoState: DuoState) {
        updateViewState { copy(showProgress = true, showDuoWebViewSheet = false) }
        launch {
            val (token, duoCookie) = authToken to passboltDuoCookieUuid
            if (token != null && duoCookie != null) {
                when (
                    val result =
                        verifyDuoCallbackUseCase.execute(
                            VerifyDuoCallbackUseCase.Input(
                                jwtHeader = token,
                                passboltDuoCookieUuid = duoCookie,
                                duoState = duoState.state,
                                duoCode = duoState.duoCode,
                            ),
                        )
                ) {
                    is VerifyDuoCallbackUseCase.Output.Error ->
                        emitSideEffect(ShowErrorSnackbar(GENERIC))
                    is VerifyDuoCallbackUseCase.Output.Failure<*> ->
                        emitSideEffect(ShowErrorSnackbar(GENERIC))
                    is VerifyDuoCallbackUseCase.Output.Unauthorized -> {
                        if (backgroundSessionRefreshSucceeded()) {
                            verifyDuoAuth(duoState)
                            return@launch
                        } else {
                            emitSideEffect(NavigateToLogin)
                        }
                    }
                    is VerifyDuoCallbackUseCase.Output.Success -> duoSuccess(result.mfaHeader)
                }
            } else {
                Timber.e("Authentication token or duo uuid cookie is null")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun duoSuccess(mfaHeader: String?) {
        mfaHeader?.let {
            emitSideEffect(NotifyVerificationSucceeded(it))
        } ?: run {
            emitSideEffect(ShowErrorSnackbar(GENERIC))
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() = refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    private fun signOutAndClose() {
        launch {
            updateViewState { copy(showProgress = true) }
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(CloseAndNavigateToStartup)
        }
    }
}
