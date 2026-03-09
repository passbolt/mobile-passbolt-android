package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Failure
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.NetworkFailure
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Success
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Unauthorized
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.WrongCode
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.PasteFromClipboard
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ValidateOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.ClearOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.PasteOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.NETWORK
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.SESSION_EXPIRED
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.WRONG_CODE
import kotlinx.coroutines.delay
import timber.log.Timber

class EnterTotpViewModel(
    hasOtherProvider: Boolean,
    private val authToken: String?,
    private val signOutUseCase: SignOutUseCase,
    private val verifyTotpUseCase: VerifyTotpUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
) : SideEffectViewModel<EnterTotpState, EnterTotpSideEffect>(
        EnterTotpState(hasOtherProvider = hasOtherProvider),
    ) {
    fun onIntent(intent: EnterTotpIntent) {
        when (intent) {
            is PasteFromClipboard -> emitSideEffect(PasteOtp)
            is ChooseOtherProvider -> emitSideEffect(NotifyChooseOtherProvider(authToken))
            is ToggleRememberMe -> updateViewState { copy(rememberMe = intent.checked) }
            is ValidateOtp -> validateOtp(intent.otp)
            is Close -> signOutAndClose()
        }
    }

    private fun validateOtp(otp: String) {
        Timber.d("Verifying TOTP")
        updateViewState { copy(showProgress = true) }
        launch {
            when (
                val result =
                    verifyTotpUseCase.execute(
                        VerifyTotpUseCase.Input(otp, authToken.orEmpty(), viewState.value.rememberMe),
                    )
            ) {
                is Failure<*> -> genericError()
                is NetworkFailure -> networkError()
                is Success -> otpSuccess(result.mfaHeader)
                is WrongCode -> totpError()
                is Unauthorized -> {
                    if (backgroundSessionRefreshSucceeded()) {
                        validateOtp(otp)
                        return@launch
                    } else {
                        emitSideEffect(ShowErrorSnackbar(SESSION_EXPIRED))
                        emitSideEffect(NavigateToLogin)
                    }
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() = refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    private fun otpSuccess(mfaHeader: String?) {
        mfaHeader?.let {
            emitSideEffect(NotifyVerificationSucceeded(it))
        } ?: run {
            emitSideEffect(ShowErrorSnackbar(GENERIC))
        }
    }

    private fun genericError() {
        emitSideEffect(ClearOtp)
        emitSideEffect(ShowErrorSnackbar(GENERIC))
    }

    private fun networkError() {
        emitSideEffect(ClearOtp)
        emitSideEffect(ShowErrorSnackbar(NETWORK))
    }

    private fun totpError() {
        launch {
            updateViewState { copy(otpTextColor = EnterTotpState.OtpTextColor.ERROR) }
            delay(CLEAR_INPUT_DELAY_MILLIS)
            updateViewState { copy(otpTextColor = EnterTotpState.OtpTextColor.DEFAULT) }
            emitSideEffect(ClearOtp)
        }
        emitSideEffect(ShowErrorSnackbar(WRONG_CODE))
    }

    private fun signOutAndClose() {
        launch {
            updateViewState { copy(showProgress = true) }
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(CloseAndNavigateToStartup)
        }
    }

    private companion object {
        private const val CLEAR_INPUT_DELAY_MILLIS = 1000L
    }
}
