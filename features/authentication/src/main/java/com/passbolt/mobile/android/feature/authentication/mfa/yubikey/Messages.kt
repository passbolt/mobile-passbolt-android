package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

import android.content.Context
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.EMPTY_OTP
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.SESSION_EXPIRED
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getSnackbarMessage(
    context: Context,
    kind: SnackbarErrorType,
): String =
    when (kind) {
        GENERIC -> context.getString(LocalizationR.string.unknown_error)
        SESSION_EXPIRED -> context.getString(LocalizationR.string.session_expired)
        EMPTY_OTP -> context.getString(LocalizationR.string.dialog_mfa_scan_empty_otp)
    }
