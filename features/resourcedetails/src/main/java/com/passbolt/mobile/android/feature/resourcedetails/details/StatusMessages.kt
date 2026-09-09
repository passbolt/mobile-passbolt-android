package com.passbolt.mobile.android.feature.resourcedetails.details

import android.content.Context
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.CANNOT_PERFORM_ACTION
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.DATA_REFRESH_ERROR
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.GENERAL_ERROR
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.INVALID_TOTP_PARAMETERS
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.TOGGLE_FAVOURITE_FAILURE
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType.TOGGLE_OFFLINE_AVAILABILITY_FAILURE
import com.passbolt.mobile.android.feature.resourcedetails.details.SuccessSnackbarType.RESOURCE_AVAILABLE_OFFLINE
import com.passbolt.mobile.android.feature.resourcedetails.details.SuccessSnackbarType.RESOURCE_EDITED
import com.passbolt.mobile.android.feature.resourcedetails.details.SuccessSnackbarType.RESOURCE_MARKED_OFFLINE_NOT_CACHED
import com.passbolt.mobile.android.feature.resourcedetails.details.SuccessSnackbarType.RESOURCE_OFFLINE_AVAILABILITY_REMOVED
import com.passbolt.mobile.android.feature.resourcedetails.details.SuccessSnackbarType.RESOURCE_SHARED
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getSuccessSnackbarMessage(
    context: Context,
    type: SuccessSnackbarType,
): String =
    when (type) {
        RESOURCE_EDITED -> context.getString(LocalizationR.string.common_message_resource_edited, "")
        RESOURCE_SHARED -> context.getString(LocalizationR.string.common_message_resource_shared)
        RESOURCE_AVAILABLE_OFFLINE -> context.getString(LocalizationR.string.offline_resource_available)
        RESOURCE_MARKED_OFFLINE_NOT_CACHED -> context.getString(LocalizationR.string.offline_resource_marked_not_cached)
        RESOURCE_OFFLINE_AVAILABILITY_REMOVED -> context.getString(LocalizationR.string.offline_resource_availability_removed)
    }

internal fun getErrorSnackbarMessage(
    context: Context,
    type: ErrorSnackbarType,
): String =
    when (type) {
        DECRYPTION_FAILURE -> context.getString(LocalizationR.string.common_decryption_failure)
        FETCH_FAILURE -> context.getString(LocalizationR.string.common_fetch_failure)
        GENERAL_ERROR -> context.getString(LocalizationR.string.common_failure_format, "")
        DATA_REFRESH_ERROR -> context.getString(LocalizationR.string.common_data_refresh_error)
        TOGGLE_FAVOURITE_FAILURE -> context.getString(LocalizationR.string.favourites_failure)
        TOGGLE_OFFLINE_AVAILABILITY_FAILURE -> context.getString(LocalizationR.string.offline_toggle_failure)
        CANNOT_PERFORM_ACTION -> context.getString(LocalizationR.string.common_lack_shared_key_access)
        INVALID_TOTP_PARAMETERS -> context.getString(LocalizationR.string.common_invalid_totp_parameters)
    }

internal fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    when (type) {
        ToastType.CONTENT_NOT_AVAILABLE -> context.getString(LocalizationR.string.content_not_available)
    }
