package com.passbolt.mobile.android.entity.permission

import com.passbolt.mobile.android.entity.resource.Permission

data class UserPermission(
    val userId: String,
    val permission: Permission,
    val permissionId: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val userName: String,
    val fingerprint: String,
    val disabled: Boolean,
)
