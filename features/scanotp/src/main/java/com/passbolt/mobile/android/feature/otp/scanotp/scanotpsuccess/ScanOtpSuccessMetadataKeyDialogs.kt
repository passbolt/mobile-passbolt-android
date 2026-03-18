package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import androidx.compose.runtime.Composable
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.TrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissNewMetadataTrustDialog
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissTrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustedMetadataKeyDeleted

@Composable
internal fun MetadataKeyDialogs(
    state: ScanOtpSuccessState,
    onIntent: (ScanOtpSuccessIntent) -> Unit,
) {
    if (state.showNewMetadataTrustDialog && state.metadataKeyToTrust != null) {
        NewMetadataKeyTrustDialog(
            newKeyToTrustModel = state.metadataKeyToTrust,
            onTrustClick = { onIntent(TrustNewMetadataKey) },
            onDismiss = { onIntent(DismissNewMetadataTrustDialog) },
        )
    }

    if (state.showTrustedMetadataKeyDeletedDialog && state.metadataKeyDeleted != null) {
        TrustedMetadataKeyDeletedDialog(
            trustedKeyDeletedModel = state.metadataKeyDeleted,
            onTrustClick = { onIntent(TrustedMetadataKeyDeleted) },
            onDismiss = { onIntent(DismissTrustedMetadataKeyDeletedDialog) },
        )
    }
}
