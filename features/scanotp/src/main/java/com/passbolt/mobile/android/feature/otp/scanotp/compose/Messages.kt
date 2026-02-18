package com.passbolt.mobile.android.feature.otp.scanotp.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage.CAMERA_ERROR
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage.CENTER_CAMERA_ON_BARCODE
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage.NOT_A_OTP_QR
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage.SCAN_ERROR
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun getTooltipMessage(
    tooltipMessage: ScanOtpState.TooltipMessage,
    scanErrorMessage: String?,
): String =
    when (tooltipMessage) {
        CENTER_CAMERA_ON_BARCODE -> stringResource(LocalizationR.string.scan_qr_aim_at_qr_code)
        MULTIPLE_BARCODES -> stringResource(LocalizationR.string.scan_qr_multiple_codes_in_range)
        NOT_A_OTP_QR -> stringResource(LocalizationR.string.otp_scan_not_a_totp_qr)
        CAMERA_ERROR -> stringResource(LocalizationR.string.scan_qr_camera_error)
        SCAN_ERROR -> {
            val base = stringResource(LocalizationR.string.scan_qr_scanning_error)
            if (!scanErrorMessage.isNullOrBlank()) "$base($scanErrorMessage)" else base
        }
    }
