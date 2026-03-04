package com.passbolt.mobile.android.feature.otp.scanotp.compose

import com.passbolt.mobile.android.ui.OtpParseResult

// TODO MOB-3691: Remove interface - replace with Compose navigation actions
interface ScanOtpNavigation {
    fun navigateBack()

    fun navigateToSuccess(totpQr: OtpParseResult.OtpQr.TotpQr)

    fun setResultAndNavigateBack(totpQr: OtpParseResult.OtpQr.TotpQr)

    fun setManualCreationResultAndNavigateBack()
}
