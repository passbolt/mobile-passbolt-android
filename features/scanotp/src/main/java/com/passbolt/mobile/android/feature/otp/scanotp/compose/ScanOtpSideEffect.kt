package com.passbolt.mobile.android.feature.otp.scanotp.compose

import com.passbolt.mobile.android.ui.OtpParseResult

internal sealed interface ScanOtpSideEffect {
    data object RequestCameraPermission : ScanOtpSideEffect

    data class NavigateToSuccess(
        val totpQr: OtpParseResult.OtpQr.TotpQr,
    ) : ScanOtpSideEffect

    data class SetResultAndNavigateBack(
        val totpQr: OtpParseResult.OtpQr.TotpQr,
    ) : ScanOtpSideEffect

    data object SetManualCreationResultAndNavigateBack : ScanOtpSideEffect

    data object NavigateToAppSettings : ScanOtpSideEffect

    data object NavigateBack : ScanOtpSideEffect
}
