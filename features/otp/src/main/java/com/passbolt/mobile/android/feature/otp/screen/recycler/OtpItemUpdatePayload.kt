package com.passbolt.mobile.android.feature.otp.screen.recycler

import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItemUpdatePayload.ALL
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItemUpdatePayload.DATA

/**
 * Informs which part of the OtpItemWrapper should be updated
 * @property DATA updates TOTP data only
 * @property ALL updates data and icon
 */
enum class OtpItemUpdatePayload {
    DATA,
    ALL,
}
