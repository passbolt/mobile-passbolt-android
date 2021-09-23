package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.mvp.BaseContract

interface EnterTotpContract {

    interface View : BaseContract.View {
        fun showProgress()
        fun hideProgress()
        fun navigateToYubikey()
        fun closeAndNavigateToStartup()
        fun pasteOtp(otp: String)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun otherProviderClick()
        fun rememberMeCheckChanged(isChecked: Boolean)
        fun closeClick()
        fun otpEntered(otp: String)
        fun pasteButtonClick(pasteData: CharSequence?)
    }
}
