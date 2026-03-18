package com.passbolt.mobile.android.permissions.userpermissionsdetails

import com.passbolt.mobile.android.ui.PermissionModelUi

sealed interface UserPermissionsSideEffect {
    data object NavigateBack : UserPermissionsSideEffect

    data class SetUpdatedPermissionResult(
        val permission: PermissionModelUi.UserPermissionModel,
    ) : UserPermissionsSideEffect

    data class SetDeletePermissionResult(
        val permission: PermissionModelUi.UserPermissionModel,
    ) : UserPermissionsSideEffect
}
