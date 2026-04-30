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

package com.passbolt.mobile.android.permissions.permissions

import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode

sealed interface PermissionsSideEffect {
    data object NavigateBack : PermissionsSideEffect

    data class NavigateToGroupPermissionDetails(
        val permission: GroupPermissionModel,
        val mode: PermissionsMode,
    ) : PermissionsSideEffect

    data class NavigateToUserPermissionDetails(
        val permission: UserPermissionModel,
        val mode: PermissionsMode,
    ) : PermissionsSideEffect

    data class NavigateToSelectShareRecipients(
        val groups: List<GroupPermissionModel>,
        val users: List<UserPermissionModel>,
    ) : PermissionsSideEffect

    data class NavigateToSelfWithMode(
        val id: String,
        val mode: PermissionsMode,
        val permissionsItem: PermissionsItem,
    ) : PermissionsSideEffect

    data object CloseWithShareSuccess : PermissionsSideEffect

    data object NavigateToHome : PermissionsSideEffect

    data class ShowErrorSnackbar(
        val type: SnackbarErrorType,
    ) : PermissionsSideEffect

    data class ShowSuccessSnackbar(
        val type: SnackbarSuccessType,
    ) : PermissionsSideEffect

    data object ShowContentNotAvailable : PermissionsSideEffect
}

enum class SnackbarErrorType {
    ONE_OWNER_REQUIRED,
    SHARE_SIMULATION_FAILED,
    SHARE_FAILED,
    SECRET_FETCH_FAILURE,
    SECRET_ENCRYPT_FAILURE,
    SECRET_DECRYPT_FAILURE,
    DATA_REFRESH_ERROR,
    GENERIC_ERROR,
    ENCRYPTION_ERROR,
    JSON_RESOURCE_SCHEMA_ERROR,
    JSON_SECRET_SCHEMA_ERROR,
    CANNOT_UPDATE_TOTP_WITH_CURRENT_CONFIG,
    FAILED_TO_VERIFY_METADATA_KEY,
    FAILED_TO_TRUST_METADATA_KEY,
    CANNOT_SHARE_RESOURCE,
}

enum class SnackbarSuccessType {
    METADATA_KEY_IS_TRUSTED,
}
