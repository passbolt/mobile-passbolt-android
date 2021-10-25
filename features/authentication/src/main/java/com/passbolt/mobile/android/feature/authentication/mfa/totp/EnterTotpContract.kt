package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.mvp.BaseContract

interface EnterTotpContract {

    interface View : BaseContract.View {
        fun showProgress()
        fun hideProgress()
        fun navigateToYubikey()
        fun closeAndNavigateToStartup()
        fun pasteOtp(otp: String)
        fun notifyVerificationSucceeded(mfaHeader: String)
        fun showError()
        fun clearInput()
        fun showWrongCodeError()
        fun showChangeProviderButton(hasYubikeyProvider: Boolean)
        fun navigateToLogin()
        fun close()
        fun notifyLoginSucceeded()
        fun setTotpInputRed()
        fun setTotpInputBlack()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun otherProviderClick()
        fun closeClick()
        fun otpEntered(otp: String, authToken: String, rememberMeChecked: Boolean)
        fun pasteButtonClick(pasteData: CharSequence?)
        fun viewCreated(hasYubikeyProvider: Boolean)
        fun authenticationSucceeded()
    }
}
