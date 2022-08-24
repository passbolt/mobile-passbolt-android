package com.passbolt.mobile.android.entity.permission

import com.passbolt.mobile.android.entity.resource.Permission

data class GroupPermission(
    val groupId: String,
    val permission: Permission,
    val permissionId: String,
    val groupName: String
)
