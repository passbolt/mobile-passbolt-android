package com.passbolt.mobile.android.permissions.navigation

import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel

data class GroupPermissionModifiedResult(
    val permission: GroupPermissionModel,
)

data class GroupPermissionDeletedResult(
    val permission: GroupPermissionModel,
)

data class UserPermissionModifiedResult(
    val permission: UserPermissionModel,
)

data class UserPermissionDeletedResult(
    val permission: UserPermissionModel,
)

data class ShareRecipientsAddedResult(
    val permissions: List<PermissionModelUi>?,
)
