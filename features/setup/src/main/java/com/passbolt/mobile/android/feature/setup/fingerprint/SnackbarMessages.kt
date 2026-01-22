package com.passbolt.mobile.android.feature.setup.fingerprint

import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.feature.setup.fingerprint.SnackbarErrorType.AUTHENTICATION_GENERIC
import com.passbolt.mobile.android.feature.setup.fingerprint.SnackbarErrorType.AUTHENTICATION_LOCKOUT
import com.passbolt.mobile.android.feature.setup.fingerprint.SnackbarErrorType.AUTHENTICATION_LOCKOUT_PERMANENT
import com.passbolt.mobile.android.feature.setup.fingerprint.SnackbarErrorType.GENERIC_ERROR

internal fun getSnackbarMessage(
    errorType: SnackbarErrorType,
    environment: FingerprintSetupEnvironment,
): String =
    when (errorType) {
        GENERIC_ERROR -> environment.context.getString(R.string.common_failure)
        AUTHENTICATION_LOCKOUT -> environment.context.getString(R.string.fingerprint_biometric_error_blocked)
        AUTHENTICATION_LOCKOUT_PERMANENT -> environment.context.getString(R.string.fingerprint_biometric_error_too_many_attempts)
        AUTHENTICATION_GENERIC -> environment.context.getString(R.string.fingerprint_biometric_error_generic)
    }
