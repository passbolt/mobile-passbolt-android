package com.passbolt.mobile.android.permissions.userpermissionsdetails

import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel

interface UserPermissionsNavigation {
    fun navigateBack()

    fun setUpdatedPermissionResult(permission: UserPermissionModel)

    fun setDeletePermissionResult(permission: UserPermissionModel)
}
