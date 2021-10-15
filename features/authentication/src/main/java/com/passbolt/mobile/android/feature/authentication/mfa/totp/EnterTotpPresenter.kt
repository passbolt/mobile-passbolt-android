package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnterTotpPresenter(
    private val signOutUseCase: SignOutUseCase,
    private val verifyTotpUseCase: VerifyTotpUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : EnterTotpContract.Presenter {

    override var view: EnterTotpContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun otherProviderClick() {
        view?.navigateToYubikey()
    }

    override fun onCreate(hasYubikeyProvider: Boolean) {
        view?.showChangeProviderButton(hasYubikeyProvider)
    }

    override fun closeClick() {
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.closeAndNavigateToStartup()
        }
    }

    override fun otpEntered(otp: String, authToken: String, rememberMeChecked: Boolean) {
        view?.showProgress()
        scope.launch {
            when (val result = verifyTotpUseCase.execute(VerifyTotpUseCase.Input(otp, authToken, rememberMeChecked))) {
                VerifyTotpUseCase.Output.Failure -> genericError()
                is VerifyTotpUseCase.Output.Success -> otpSuccess(result.mfaHeader)
                VerifyTotpUseCase.Output.WrongCode -> totpError()
            }
            view?.hideProgress()
        }
    }

    private fun otpSuccess(mfaHeader: String?) {
        mfaHeader?.let {
            view?.notifyVerificationSucceeded(it)
        } ?: run {
            view?.showError()
        }
    }

    private fun genericError() {
        view?.clearInput()
        view?.showError()
    }

    private fun totpError() {
        view?.clearInput()
        view?.showWrongCodeError()
    }

    override fun pasteButtonClick(pasteData: CharSequence?) {
        pasteData.removeWhiteSpace()?.let {
            if (it.isNotBlank() && it.length == OTP_LENGTH && it.all { character -> character.isDigit() }) {
                view?.pasteOtp(it)
            }
        }
    }

    override fun inputTextChange() {
        view?.hideWrongCodeError()
    }

    private fun CharSequence?.removeWhiteSpace() = this?.let {
        replace("\\s".toRegex(), "")
    }

    private companion object {
        private const val OTP_LENGTH = 6
    }
}
