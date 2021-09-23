package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnterTotpPresenter(
    private val signOutUseCase: SignOutUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : EnterTotpContract.Presenter {

    override var view: EnterTotpContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun otherProviderClick() {
        view?.navigateToYubikey()
    }

    override fun rememberMeCheckChanged(isChecked: Boolean) {
        // TODO
    }

    override fun closeClick() {
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.closeAndNavigateToStartup()
        }
    }

    override fun otpEntered(otp: String) {
        // TODO verify otp using endpoint
    }

    override fun pasteButtonClick(pasteData: CharSequence?) {
        pasteData.removeWhiteSpace()?.let {
            if (it.isNotBlank() && it.length == OTP_LENGTH && it.all { character -> character.isDigit() }) {
                view?.pasteOtp(it)
            }
        }
    }

    private fun CharSequence?.removeWhiteSpace() = this?.let {
        replace("\\s".toRegex(), "")
    }

    private companion object {
        private const val OTP_LENGTH = 6
    }
}
