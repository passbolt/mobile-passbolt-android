package com.passbolt.mobile.android.permissions.userpermissionsdetails

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.userPermissionsModule() {
    viewModel { params ->
        UserPermissionsViewModel(
            mode = params.get(),
            permission = params.get(),
            getLocalUserUseCase = get(),
            coroutineLaunchContext = get(),
        )
    }
}
