package com.passbolt.mobile.android.feature.authentication.mfa.youbikey

import com.passbolt.mobile.android.core.mvp.BaseContract

interface ScanYubikeyContract {

    interface View : BaseContract.View {
        fun showScanYubikey()
        fun showEmptyScannedOtp()
        fun showScanOtpCancelled()
        fun closeAndNavigateToStartup()
        fun showProgress()
        fun hideProgress()
        fun notifyVerificationSucceeded(mfaHeader: String)
        fun showError()
        fun showChangeProviderButton(bundledHasTotpProvider: Boolean)
        fun close()
        fun navigateToLogin()
        fun notifyLoginSucceeded()
        fun showSessionExpired()
        fun showYubikeyDoesNotBelongToCurrentUser()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun scanYubikeyClick()
        fun yubikeyScanned(otp: String?, authToken: String?, rememberChecked: Boolean)
        fun yubikeyScanCancelled()
        fun closeClick()
        fun onViewCreated(bundledOtherProvider: Boolean)
        fun authenticationSucceeded()
    }
}
