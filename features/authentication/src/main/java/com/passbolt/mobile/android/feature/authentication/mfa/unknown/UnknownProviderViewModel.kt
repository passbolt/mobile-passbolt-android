package com.passbolt.mobile.android.feature.authentication.mfa.unknown

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderSideEffect.CloseAndNavigateToStartup

class UnknownProviderViewModel(
    private val signOutUseCase: SignOutUseCase,
) : SideEffectViewModel<UnknownProviderState, UnknownProviderSideEffect>(
        UnknownProviderState(),
    ) {
    fun onIntent(intent: UnknownProviderIntent) {
        when (intent) {
            is Close -> signOutAndClose()
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
