package com.passbolt.mobile.android.resourcepicker.screen

import android.content.Context
import com.passbolt.mobile.android.resourcepicker.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.resourcepicker.screen.SnackbarErrorType.NO_PERMISSION
import com.passbolt.mobile.android.resourcepicker.screen.SnackbarErrorType.UNSUPPORTED_RESOURCE_TYPE
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getErrorMessage(
    context: Context,
    type: SnackbarErrorType,
): String =
    when (type) {
        FAILED_TO_REFRESH_DATA -> context.getString(LocalizationR.string.common_data_refresh_error)
        NO_PERMISSION -> context.getString(LocalizationR.string.resource_picker_no_edit_permission)
        UNSUPPORTED_RESOURCE_TYPE -> context.getString(LocalizationR.string.resource_picker_resource_not_compatible)
    }
