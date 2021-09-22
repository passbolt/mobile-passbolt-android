package com.passbolt.mobile.android.feature.authentication.mfa.youbikey

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class ScanYubikeyPresenter(
    private val signOutUseCase: SignOutUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : ScanYubikeyContract.Presenter {

    override var view: ScanYubikeyContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun scanYubikeyClick() {
        view?.showScanYubikey()
    }

    override fun yubikeyScanned(otp: String?) {
        if (!otp.isNullOrBlank()) {
            // TODO pass oth to verification endpoint
            // TODO and call dialog listener
        } else {
            view?.showEmptyScannedOtp()
        }
    }

    override fun yubikeyScanCancelled() {
        view?.showScanOtpCancelled()
    }

    override fun rememberMeCheckChanged(isChecked: Boolean) {
        // TODO
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
