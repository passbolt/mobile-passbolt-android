package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import android.content.Context
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.CANNOT_CREATE_WITH_CURRENT_CONFIG
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.ENCRYPTION_ERROR
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.FAILED_TO_TRUST_METADATA_KEY
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.FAILED_TO_VERIFY_METADATA_KEY
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.GENERIC_ERROR
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.JSON_RESOURCE_SCHEMA_VALIDATION_ERROR
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ErrorSnackbarType.JSON_SECRET_SCHEMA_VALIDATION_ERROR
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.SuccessSnackbarType.NEW_METADATA_KEY_IS_TRUSTED
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getErrorSnackbarMessage(
    context: Context,
    sideEffect: ShowErrorSnackbar,
): String =
    when (sideEffect.type) {
        GENERIC_ERROR -> context.getString(LocalizationR.string.common_failure)
        ENCRYPTION_ERROR -> context.getString(LocalizationR.string.common_encryption_failure)
        JSON_RESOURCE_SCHEMA_VALIDATION_ERROR -> context.getString(LocalizationR.string.common_json_schema_resource_validation_error)
        JSON_SECRET_SCHEMA_VALIDATION_ERROR -> context.getString(LocalizationR.string.common_json_schema_secret_validation_error)
        CANNOT_CREATE_WITH_CURRENT_CONFIG -> context.getString(LocalizationR.string.common_cannot_create_resource_with_current_config)
        FAILED_TO_VERIFY_METADATA_KEY -> context.getString(LocalizationR.string.common_metadata_key_verification_failure)
        FAILED_TO_TRUST_METADATA_KEY -> context.getString(LocalizationR.string.common_metadata_key_trust_failed)
    }

internal fun getSuccessSnackbarMessage(
    context: Context,
    sideEffect: ShowSuccessSnackbar,
): String =
    when (sideEffect.type) {
        NEW_METADATA_KEY_IS_TRUSTED -> context.getString(LocalizationR.string.common_metadata_key_is_trusted)
    }
