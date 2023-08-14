package com.passbolt.mobile.android.ui

import android.content.Context
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
enum class ResourcePermission {
    READ,
    UPDATE,
    OWNER;

    companion object {
        fun getPermissionTextValue(context: Context, permission: ResourcePermission) = context.getString(
            when (permission) {
                READ -> LocalizationR.string.resource_permissions_can_read
                UPDATE -> LocalizationR.string.resource_permissions_can_update
                OWNER -> LocalizationR.string.resource_permissions_is_owner
            }
        )

        fun getPermissionIcon(context: Context, permission: ResourcePermission) = context.getDrawable(
            when (permission) {
                READ -> CoreUiR.drawable.ic_permission_read
                UPDATE -> CoreUiR.drawable.ic_permission_edit
                OWNER -> CoreUiR.drawable.ic_permission_owner
            }
        )
    }
}
