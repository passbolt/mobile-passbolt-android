package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.groupPermissionsModule() {
    viewModel { params ->
        GroupPermissionsViewModel(
            permission = params.get(),
            mode = params.get(),
            getGroupWithUsersUseCase = get(),
            coroutineLaunchContext = get(),
        )
    }
}
