package com.passbolt.mobile.android.feature.otp.scanotp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface ScanOtpNavigationKey : NavKey {
    @Serializable
    data object Scanning : ScanOtpNavigationKey

    @Serializable
    data object Success : ScanOtpNavigationKey
}
