package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.AuthenticateWithWebauthn
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyOtherProviderClicked
import timber.log.Timber

class AuthWithWebauthnViewModel(
    hasOtherProvider: Boolean,
    private val isSetupFlow: Boolean,
    private val authToken: String?,
    private val signOutUseCase: SignOutUseCase,
) : SideEffectViewModel<AuthWithWebauthnState, AuthWithWebauthnSideEffect>(
        AuthWithWebauthnState(hasOtherProvider = hasOtherProvider),
    ) {
    fun onIntent(intent: AuthWithWebauthnIntent) {
        when (intent) {
            is AuthenticateWithWebauthn -> {
                // TODO
                Timber.d("Webauthn verification requested")
            }
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

    private fun signOutAndClose() {
        launch {
            updateViewState { copy(showProgress = true) }
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(CloseAndNavigateToStartup)
        }
    }
}
