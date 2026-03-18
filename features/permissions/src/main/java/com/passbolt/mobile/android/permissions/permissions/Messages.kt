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

import android.content.Context
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.CANNOT_SHARE_RESOURCE
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.CANNOT_UPDATE_TOTP_WITH_CURRENT_CONFIG
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.DATA_REFRESH_ERROR
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.ENCRYPTION_ERROR
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEY
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.GENERIC_ERROR
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.JSON_RESOURCE_SCHEMA_ERROR
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.JSON_SECRET_SCHEMA_ERROR
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.ONE_OWNER_REQUIRED
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.SECRET_DECRYPT_FAILURE
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.SECRET_ENCRYPT_FAILURE
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.SECRET_FETCH_FAILURE
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.SHARE_FAILED
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.SHARE_SIMULATION_FAILED
import com.passbolt.mobile.android.permissions.permissions.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Suppress("CyclomaticComplexMethod")
internal fun getErrorMessage(
    context: Context,
    type: SnackbarErrorType,
): String =
    context.getString(
        when (type) {
            ONE_OWNER_REQUIRED -> LocalizationR.string.resource_permissions_one_owner
            SHARE_SIMULATION_FAILED -> LocalizationR.string.resource_permissions_share_simulation_failed
            SHARE_FAILED -> LocalizationR.string.resource_permissions_share_failed
            SECRET_FETCH_FAILURE -> LocalizationR.string.common_fetch_failure
            SECRET_ENCRYPT_FAILURE -> LocalizationR.string.common_encryption_failure
            SECRET_DECRYPT_FAILURE -> LocalizationR.string.common_decryption_failure
            DATA_REFRESH_ERROR -> LocalizationR.string.common_data_refresh_error
            GENERIC_ERROR -> LocalizationR.string.common_failure
            ENCRYPTION_ERROR -> LocalizationR.string.common_encryption_failure
            JSON_RESOURCE_SCHEMA_ERROR -> LocalizationR.string.common_json_schema_resource_validation_error
            JSON_SECRET_SCHEMA_ERROR -> LocalizationR.string.common_json_schema_secret_validation_error
            CANNOT_UPDATE_TOTP_WITH_CURRENT_CONFIG -> LocalizationR.string.common_cannot_create_resource_with_current_config
            FAILED_TO_VERIFY_METADATA_KEY -> LocalizationR.string.common_metadata_key_verification_failure
            FAILED_TO_TRUST_METADATA_KEY -> LocalizationR.string.common_metadata_key_trust_failed
            CANNOT_SHARE_RESOURCE -> LocalizationR.string.common_lack_shared_key_access
        },
    )

internal fun getSuccessMessage(
    context: Context,
    type: SnackbarSuccessType,
): String =
    context.getString(
        when (type) {
            METADATA_KEY_IS_TRUSTED -> LocalizationR.string.common_metadata_key_is_trusted
        },
    )
