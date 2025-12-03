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
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_DELETED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.feature.otp.screen.ToastType.WAIT_FOR_DATA_REFRESH_FINISH
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    when (type) {
        WAIT_FOR_DATA_REFRESH_FINISH -> context.getString(LocalizationR.string.home_please_wait_for_refresh)
    }

internal fun getSuccessMessage(
    context: Context,
    type: SnackbarSuccessType,
    additionalSuccessMessage: String? = null,
): String =
    when (type) {
        RESOURCE_EDITED ->
            context.getString(
                R.string.common_message_resource_edited,
                additionalSuccessMessage.orEmpty(),
            )
        RESOURCE_CREATED -> context.getString(R.string.resource_form_create_success)
        RESOURCE_DELETED -> context.getString(R.string.otp_deleted)
        METADATA_KEY_IS_TRUSTED -> context.getString(R.string.common_metadata_key_is_trusted)
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
        NO_SHARED_KEY_ACCESS -> context.getString(LocalizationR.string.common_lack_shared_key_access)
    }
