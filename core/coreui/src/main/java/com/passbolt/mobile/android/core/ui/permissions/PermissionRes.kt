package com.passbolt.mobile.android.core.ui.permissions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ
import com.passbolt.mobile.android.ui.ResourcePermission.UPDATE
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@DrawableRes
internal fun getPermissionIconRes(permission: ResourcePermission): Int =
    when (permission) {
        READ -> CoreUiR.drawable.ic_permission_read
        UPDATE -> CoreUiR.drawable.ic_permission_edit
        OWNER -> CoreUiR.drawable.ic_permission_owner
    }

@StringRes
internal fun getPermissionNameRes(permission: ResourcePermission): Int =
    when (permission) {
        READ -> LocalizationR.string.resource_permissions_can_read
        UPDATE -> LocalizationR.string.resource_permissions_can_update
        OWNER -> LocalizationR.string.resource_permissions_is_owner
    }
