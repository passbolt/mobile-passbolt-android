package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel

interface GroupPermissionsNavigation {
    fun navigateBack()

    fun navigateToGroupMembers(groupId: String)

    fun setUpdatedPermissionResult(permission: GroupPermissionModel)

    fun setDeletePermissionResult(permission: GroupPermissionModel)
}
