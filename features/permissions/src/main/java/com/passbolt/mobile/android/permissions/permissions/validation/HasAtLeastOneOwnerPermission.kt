package com.passbolt.mobile.android.permissions.permissions.validation

import com.passbolt.mobile.android.common.validation.Rule
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission

object HasAtLeastOneOwnerPermission : Rule<MutableList<PermissionModelUi>>(
    { it.any { p -> p.permission == ResourcePermission.OWNER } },
)
