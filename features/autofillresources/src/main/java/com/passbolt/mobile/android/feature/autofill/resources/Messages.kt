package com.passbolt.mobile.android.feature.autofill.resources

import android.content.Context
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.FETCH_FAILURE
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    when (type) {
        DECRYPTION_FAILURE -> context.getString(LocalizationR.string.common_decryption_failure)
        FETCH_FAILURE -> context.getString(LocalizationR.string.common_fetch_failure)
    }
