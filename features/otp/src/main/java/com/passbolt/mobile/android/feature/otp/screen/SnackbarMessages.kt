package com.passbolt.mobile.android.feature.otp.screen

import android.content.Context
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.ENCRYPTION_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.ERROR
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEYS
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getSuccessMessage(
    context: Context,
    type: SnackbarSuccessType,
    additionalSuccessMessage: String? = null,
): String =
    when (type) {
        SnackbarSuccessType.RESOURCE_EDITED ->
            context.getString(
                R.string.common_message_resource_edited,
                additionalSuccessMessage.orEmpty(),
            )
        SnackbarSuccessType.RESOURCE_CREATED -> context.getString(R.string.resource_form_create_success)
        SnackbarSuccessType.RESOURCE_DELETED -> context.getString(R.string.otp_deleted)
        SnackbarSuccessType.METADATA_KEY_IS_TRUSTED -> context.getString(R.string.common_metadata_key_is_trusted)
    }

internal fun getErrorMessage(
    context: Context,
    type: SnackbarErrorType,
    additionalErrorMessage: String? = null,
): String =
    when (type) {
        DECRYPTION_FAILURE ->
            context.getString(LocalizationR.string.common_decryption_failure)
        FETCH_FAILURE ->
            context.getString(LocalizationR.string.common_fetch_failure)
        ERROR ->
            context.getString(LocalizationR.string.common_failure_format, additionalErrorMessage.orEmpty())
        FAILED_TO_DELETE_RESOURCE -> context.getString(LocalizationR.string.otp_failed_to_delete)
        ENCRYPTION_FAILURE -> context.getString(LocalizationR.string.common_encryption_failure)
        RESOURCE_SCHEMA_INVALID -> context.getString(LocalizationR.string.common_json_schema_resource_validation_error)
        SECRET_SCHEMA_INVALID -> context.getString(LocalizationR.string.common_json_schema_secret_validation_error)
        CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION ->
            context.getString(
                LocalizationR.string.common_cannot_create_resource_with_current_config,
            )
        FAILED_TO_VERIFY_METADATA_KEYS -> context.getString(LocalizationR.string.common_metadata_key_verification_failure)
        FAILED_TO_TRUST_METADATA_KEY -> context.getString(LocalizationR.string.common_metadata_key_trust_failed)
        FAILED_TO_REFRESH_DATA -> context.getString(LocalizationR.string.common_data_refresh_error)
    }
