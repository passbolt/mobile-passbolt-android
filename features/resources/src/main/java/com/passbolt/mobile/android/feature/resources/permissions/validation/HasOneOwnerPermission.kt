package com.passbolt.mobile.android.feature.resources.permissions.validation

import com.passbolt.mobile.android.common.validation.Rule
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission

object HasOneOwnerPermission : Rule<MutableList<PermissionModelUi>>(
    { it.filter { p -> p.permission == ResourcePermission.OWNER }.size == 1 }
)
