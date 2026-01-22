package com.passbolt.mobile.android.feature.setup.scanqr

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.CAMERA_ERROR
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.CENTER_CAMERA_ON_BARCODE
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.KEEP_GOING
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.NOT_A_PASSBOLT_QR
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.SCAN_ERROR
import com.passbolt.mobile.android.feature.setup.scanqr.ToastType
import com.passbolt.mobile.android.feature.setup.scanqr.ToastType.UPDATE_TRANSFER_ERROR

@Composable
fun getTooltipMessage(
    message: ScanQrState.TooltipMessage,
    scanErrorMessage: String? = null,
): String =
    when (message) {
        CENTER_CAMERA_ON_BARCODE -> stringResource(R.string.scan_qr_aim_at_qr_code)
        KEEP_GOING -> stringResource(R.string.scan_qr_keep_going)
        MULTIPLE_BARCODES -> stringResource(R.string.scan_qr_multiple_codes_in_range)
        NOT_A_PASSBOLT_QR -> stringResource(R.string.scan_qr_not_a_passbolt_qr)
        CAMERA_ERROR -> stringResource(R.string.scan_qr_camera_error)
        SCAN_ERROR -> {
            val baseMessage = stringResource(R.string.scan_qr_scanning_error)
            if (!scanErrorMessage.isNullOrBlank()) {
                "$baseMessage ($scanErrorMessage)"
            } else {
                baseMessage
            }
        }
    }

fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    when (type) {
        UPDATE_TRANSFER_ERROR -> context.getString(R.string.scan_qr_update_transfer_error)
    }
