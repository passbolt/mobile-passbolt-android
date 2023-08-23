package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class EnterTotpPresenter(
    private val signOutUseCase: SignOutUseCase,
    private val verifyTotpUseCase: VerifyTotpUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : EnterTotpContract.Presenter {

    override var view: EnterTotpContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun viewCreated(hasOtherProvider: Boolean) {
        view?.showChangeProviderButton(hasOtherProvider)
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
        Timber.d("Verifying TOTP")
        view?.showProgress()
        scope.launch {
            when (val result =
                verifyTotpUseCase.execute(VerifyTotpUseCase.Input(otp, authToken, rememberMeChecked))
            ) {
                is VerifyTotpUseCase.Output.Failure<*> -> genericError()
                is VerifyTotpUseCase.Output.NetworkFailure -> networkError()
                is VerifyTotpUseCase.Output.Success -> otpSuccess(result.mfaHeader)
                is VerifyTotpUseCase.Output.WrongCode -> totpError()
                is VerifyTotpUseCase.Output.Unauthorized -> {
                    if (backgroundSessionRefreshSucceeded()) {
                        otpEntered(otp, authToken, rememberMeChecked)
                    } else {
                        view?.run {
                            showSessionExpired()
                            navigateToLogin()
                        }
                    }
                }
            }
            view?.hideProgress()
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() =
        refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    override fun authenticationSucceeded() {
        view?.close()
        view?.notifyLoginSucceeded()
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

    private fun networkError() {
        view?.clearInput()
        view?.showNetworkError()
    }

    private fun totpError() {
        scope.launch {
            view?.setTotpInputRed()
            delay(CLEAR_INPUT_DELAY)
            view?.setTotpInputBlack()
            view?.clearInput()
        }
        view?.showWrongCodeError()
    }

    override fun pasteButtonClick(pasteData: CharSequence?) {
        pasteData.removeWhiteSpace()?.let {
            if (it.isNotBlank() && it.length == OTP_LENGTH && it.all { character -> character.isDigit() }) {
                view?.pasteOtp(it)
            }
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    private fun CharSequence?.removeWhiteSpace() = this?.let {
        replace("\\s".toRegex(), "")
    }

    private companion object {
        private const val OTP_LENGTH = 6
        private const val CLEAR_INPUT_DELAY = 1000L
    }
}
