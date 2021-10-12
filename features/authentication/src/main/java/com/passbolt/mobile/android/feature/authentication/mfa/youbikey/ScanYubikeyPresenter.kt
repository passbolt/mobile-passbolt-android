package com.passbolt.mobile.android.feature.authentication.mfa.youbikey

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyYubikeyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class ScanYubikeyPresenter(
    private val signOutUseCase: SignOutUseCase,
    private val verifyYubikeyUseCase: VerifyYubikeyUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : ScanYubikeyContract.Presenter {

    override var view: ScanYubikeyContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun scanYubikeyClick() {
        view?.showScanYubikey()
    }

    override fun yubikeyScanned(otp: String?, authToken: String?, rememberChecked: Boolean) {
        if (!otp.isNullOrBlank()) {
            verifyYubikey(otp, authToken, rememberChecked)
        } else {
            view?.showEmptyScannedOtp()
        }
    }

    private fun verifyYubikey(otp: String, authToken: String?, rememberChecked: Boolean) {
        view?.showProgress()
        scope.launch {
            when (val result =
                verifyYubikeyUseCase.execute(VerifyYubikeyUseCase.Input(otp, authToken, rememberChecked))) {
                VerifyYubikeyUseCase.Output.Failure -> view?.showError()
                is VerifyYubikeyUseCase.Output.Success -> yubikeySuccess(result.mfaHeader)
            }
            view?.hideProgress()
        }
    }

    private fun yubikeySuccess(mfaHeader: String?) {
        mfaHeader?.let {
            view?.notifyVerificationSucceeded(it)
        } ?: run {
            view?.showError()
        }
    }

    override fun yubikeyScanCancelled() {
        view?.showScanOtpCancelled()
    }

    override fun otherProviderClick() {
        view?.navigateToTotp()
    }

    override fun closeClick() {
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.closeAndNavigateToStartup()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
