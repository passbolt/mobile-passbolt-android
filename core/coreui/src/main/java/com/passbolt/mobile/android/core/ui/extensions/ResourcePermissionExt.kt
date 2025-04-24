package com.passbolt.mobile.android.core.ui.extensions

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.core.localization.R as LocalizationR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

fun ResourcePermission.getPermissionTextValue(context: Context) = context.getString(
    when (this) {
        ResourcePermission.READ -> LocalizationR.string.resource_permissions_can_read
        ResourcePermission.UPDATE -> LocalizationR.string.resource_permissions_can_update
        ResourcePermission.OWNER -> LocalizationR.string.resource_permissions_is_owner
    }
)

fun ResourcePermission.getPermissionIcon(context: Context) = AppCompatResources.getDrawable(
    context,
    when (this) {
        ResourcePermission.READ -> R.drawable.ic_permission_read
        ResourcePermission.UPDATE -> R.drawable.ic_permission_edit
        ResourcePermission.OWNER -> R.drawable.ic_permission_owner
    }
)
