package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import com.passbolt.mobile.android.ui.ResourcePermission

sealed interface GroupPermissionsIntent {
    data object GoBack : GroupPermissionsIntent

    data object SeeGroupMembers : GroupPermissionsIntent

    data class SelectPermission(
        val permission: ResourcePermission,
    ) : GroupPermissionsIntent

    data object Save : GroupPermissionsIntent

    data object DeletePermission : GroupPermissionsIntent

    data object ConfirmPermissionDelete : GroupPermissionsIntent

    data object CancelPermissionDelete : GroupPermissionsIntent
}
