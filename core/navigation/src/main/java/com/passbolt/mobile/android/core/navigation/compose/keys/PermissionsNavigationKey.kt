package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import kotlinx.serialization.Serializable

sealed interface PermissionsNavigationKey : NavKey {
    @Serializable
    data class Permissions(
        val id: String,
        val mode: PermissionsMode,
        val permissionsItem: PermissionsItem,
    ) : PermissionsNavigationKey

    @Serializable
    data class GroupPermissionDetails(
        val permission: PermissionModelUi.GroupPermissionModel,
        val mode: PermissionsMode,
    ) : PermissionsNavigationKey

    @Serializable
    data class UserPermissionDetails(
        val permission: PermissionModelUi.UserPermissionModel,
        val mode: PermissionsMode,
    ) : PermissionsNavigationKey

    @Serializable
    data class PermissionRecipients(
        val userPermissions: List<PermissionModelUi.UserPermissionModel>,
        val groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
    ) : PermissionsNavigationKey
}
