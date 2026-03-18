package com.passbolt.mobile.android.permissions.userpermissionsdetails

import com.passbolt.mobile.android.ui.ResourcePermission

sealed interface UserPermissionsIntent {
    data object GoBack : UserPermissionsIntent

    data class SelectPermission(
        val permission: ResourcePermission,
    ) : UserPermissionsIntent

    data object Save : UserPermissionsIntent

    data object DeletePermission : UserPermissionsIntent

    data object ConfirmPermissionDelete : UserPermissionsIntent

    data object CancelPermissionDelete : UserPermissionsIntent
}
