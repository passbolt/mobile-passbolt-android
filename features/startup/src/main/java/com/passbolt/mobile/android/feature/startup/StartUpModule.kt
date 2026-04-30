package com.passbolt.mobile.android.feature.startup

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startUpModule =
    module {
        viewModel { params ->
            StartUpViewModel(
                accountSetupDataModel = params.getOrNull(),
                getAccountsUseCase = get(),
            )
        }
        factoryOf(::AccountSetupModelCreator)
    }
