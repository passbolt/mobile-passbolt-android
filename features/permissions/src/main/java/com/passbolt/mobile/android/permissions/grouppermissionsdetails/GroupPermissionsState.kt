package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.UserModel

data class GroupPermissionsState(
    val groupPermission: GroupPermissionModel,
    val users: List<UserModel> = emptyList(),
    val isEditMode: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
