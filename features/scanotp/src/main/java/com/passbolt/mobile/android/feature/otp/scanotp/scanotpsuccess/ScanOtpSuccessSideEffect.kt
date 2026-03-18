package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.ui.OtpParseResult

internal sealed interface ScanOtpSuccessSideEffect {
    data class NavigateToOtpList(
        val totp: OtpParseResult.OtpQr.TotpQr,
        val otpCreated: Boolean,
        val resourceId: String,
    ) : ScanOtpSuccessSideEffect

    data class NavigateToResourcePicker(
        val suggestedUri: String?,
    ) : ScanOtpSuccessSideEffect

    data class ShowErrorSnackbar(
        val type: ErrorSnackbarType,
        val message: String? = null,
    ) : ScanOtpSuccessSideEffect

    data class ShowSuccessSnackbar(
        val type: SuccessSnackbarType,
    ) : ScanOtpSuccessSideEffect
}

internal enum class ErrorSnackbarType {
    GENERIC_ERROR,
    ENCRYPTION_ERROR,
    JSON_RESOURCE_SCHEMA_VALIDATION_ERROR,
    JSON_SECRET_SCHEMA_VALIDATION_ERROR,
    CANNOT_CREATE_WITH_CURRENT_CONFIG,
    FAILED_TO_VERIFY_METADATA_KEY,
    FAILED_TO_TRUST_METADATA_KEY,
}

internal enum class SuccessSnackbarType {
    NEW_METADATA_KEY_IS_TRUSTED,
}
