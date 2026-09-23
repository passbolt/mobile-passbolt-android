package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.authWithWebauthnModule() {
    viewModel { params ->
        AuthWithWebauthnViewModel(
            authToken = params[0],
            hasOtherProvider = params[1],
            isSetupFlow = params[2],
            signOutUseCase = get(),
        )
    }
}
