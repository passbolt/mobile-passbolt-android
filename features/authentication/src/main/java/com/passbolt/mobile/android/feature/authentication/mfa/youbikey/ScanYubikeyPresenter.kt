package com.passbolt.mobile.android.feature.authentication.mfa.youbikey

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyYubikeyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

class ScanYubikeyPresenter(
    private val signOutUseCase: SignOutUseCase,
    private val verifyYubikeyUseCase: VerifyYubikeyUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
) : ScanYubikeyContract.Presenter {
    override var view: ScanYubikeyContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun scanYubikeyClick() {
        view?.showScanYubikey()
    }

    override fun onViewCreated(bundledOtherProvider: Boolean) {
        view?.showChangeProviderButton(bundledOtherProvider)
    }

    override fun yubikeyScanned(
        otp: String?,
        authToken: String?,
        rememberChecked: Boolean,
    ) {
        if (!otp.isNullOrBlank()) {
            verifyYubikey(otp, authToken, rememberChecked)
        } else {
            view?.showEmptyScannedOtp()
        }
    }

    private fun verifyYubikey(
        otp: String,
        authToken: String?,
        rememberChecked: Boolean,
    ) {
        Timber.d("Verifying Yubikey")
        view?.showProgress()
        scope.launch {
            when (
                val result =
                    verifyYubikeyUseCase.execute(VerifyYubikeyUseCase.Input(otp, authToken, rememberChecked))
            ) {
                is VerifyYubikeyUseCase.Output.Failure<*> -> view?.showError()
                is VerifyYubikeyUseCase.Output.NetworkFailure -> view?.showError()
                is VerifyYubikeyUseCase.Output.Success -> yubikeySuccess(result.mfaHeader)
                is VerifyYubikeyUseCase.Output.Unauthorized -> {
                    if (backgroundSessionRefreshSucceeded()) {
                        verifyYubikey(otp, authToken, rememberChecked)
                    } else {
                        view?.run {
                            showEmptyScannedOtp()
                            navigateToLogin()
                        }
                    }
                }
                is VerifyYubikeyUseCase.Output.YubikeyNotFromCurrentUser ->
                    view?.showYubikeyDoesNotBelongToCurrentUser()
            }
            view?.hideProgress()
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() = refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    override fun authenticationSucceeded() {
        view?.close()
        view?.notifyLoginSucceeded()
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
