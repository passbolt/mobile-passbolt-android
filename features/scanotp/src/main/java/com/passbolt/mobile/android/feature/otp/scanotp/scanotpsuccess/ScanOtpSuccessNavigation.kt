package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.ui.OtpParseResult

// TODO MOB-3691: Remove interface - replace with Compose navigation actions
interface ScanOtpSuccessNavigation {
    fun navigateToOtpList(
        totp: OtpParseResult.OtpQr.TotpQr,
        otpCreated: Boolean,
        resourceId: String,
    )

    fun navigateToResourcePicker(suggestedUri: String?)
}
