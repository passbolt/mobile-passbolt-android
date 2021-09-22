package com.passbolt.mobile.android.feature.authentication.mfa.youbikey

import com.passbolt.mobile.android.core.mvp.BaseContract

interface ScanYubikeyContract {

    interface View : BaseContract.View {
        fun showScanYubikey()
        fun showEmptyScannedOtp()
        fun showScanOtpCancelled()
        fun navigateToTotp()
        fun closeAndNavigateToStartup()
        fun showProgress()
        fun hideProgress()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun scanYubikeyClick()
        fun otherProviderClick()
        fun yubikeyScanned(otp: String?)
        fun yubikeyScanCancelled()
        fun rememberMeCheckChanged(isChecked: Boolean)
        fun closeClick()
    }
}
