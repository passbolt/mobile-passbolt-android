package com.passbolt.mobile.android.feature.otp.scanotp.compose

import com.passbolt.mobile.android.ui.OtpParseResult

interface ScanOtpNavigation {
    fun navigateBack()

    fun navigateToSuccess(totpQr: OtpParseResult.OtpQr.TotpQr)

    fun setResultAndNavigateBack(totpQr: OtpParseResult.OtpQr.TotpQr)

    fun setManualCreationResultAndNavigateBack()

    fun navigateToOtpList(
        totp: OtpParseResult.OtpQr.TotpQr,
        otpCreated: Boolean,
        resourceId: String,
    )

    fun navigateToResourcePicker(suggestedUri: String?)
}
