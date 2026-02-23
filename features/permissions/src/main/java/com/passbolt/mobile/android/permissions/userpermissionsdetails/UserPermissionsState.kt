package com.passbolt.mobile.android.permissions.userpermissionsdetails

import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.UserModel

data class UserPermissionsState(
    val permission: UserPermissionModel,
    val user: UserModel? = null,
    val isEditMode: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
