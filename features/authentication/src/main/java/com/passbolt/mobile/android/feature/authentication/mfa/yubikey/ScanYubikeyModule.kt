package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyYubikeyUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel

fun Module.scanYubikeyModule() {
    factoryOf(::VerifyYubikeyUseCase)
    viewModel { params ->
        ScanYubikeyViewModel(
            authToken = params[0],
            hasOtherProvider = params[1],
            signOutUseCase = get(),
            verifyYubikeyUseCase = get(),
            refreshSessionUseCase = get(),
        )
    }
}
