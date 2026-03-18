package com.passbolt.mobile.android.feature.otp.scanotp.compose

import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import kotlinx.coroutines.flow.StateFlow

sealed interface ScanOtpIntent {
    data class Initialize(
        val barcodeScanFlow: StateFlow<BarcodeScanResult>,
        val mode: ScanOtpMode,
    ) : ScanOtpIntent

    data class StartCameraError(
        val exception: Exception,
    ) : ScanOtpIntent

    data object RejectCameraPermission : ScanOtpIntent

    data object DismissCameraRequiredDialog : ScanOtpIntent

    data object DismissCameraPermissionRequiredDialog : ScanOtpIntent

    data object CreateTotpManually : ScanOtpIntent

    data object GoToSettings : ScanOtpIntent

    data object GoBack : ScanOtpIntent
}
