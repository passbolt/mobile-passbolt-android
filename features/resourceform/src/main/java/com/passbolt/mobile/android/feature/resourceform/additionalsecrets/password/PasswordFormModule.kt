package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

internal fun Module.passwordFormModule() {
    viewModel { params ->
        PasswordFormViewModel(
            mode = params.get(),
            passwordModel = params.get(),
            entropyViewMapper = get(),
            entropyCalculator = get(),
            getPasswordPoliciesUseCase = get(),
            secretGenerator = get(),
        )
    }
}
