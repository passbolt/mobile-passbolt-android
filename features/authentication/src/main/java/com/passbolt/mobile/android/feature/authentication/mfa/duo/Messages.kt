package com.passbolt.mobile.android.feature.authentication.mfa.duo

import android.content.Context
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType.SESSION_EXPIRED
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getSnackbarMessage(
    context: Context,
    kind: SnackbarErrorType,
): String =
    when (kind) {
        GENERIC -> context.getString(LocalizationR.string.unknown_error)
        SESSION_EXPIRED -> context.getString(LocalizationR.string.session_expired)
    }
