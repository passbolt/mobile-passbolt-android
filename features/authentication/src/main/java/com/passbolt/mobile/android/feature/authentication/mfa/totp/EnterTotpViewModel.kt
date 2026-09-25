package com.passbolt.mobile.android.feature.authentication.mfa.totp

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.authentication.auth.OneTimeSessionRefreshRetry
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Failure
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.NetworkFailure
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Success
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Unauthorized
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.WrongCode
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.DismissSetupLeave
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
import kotlin.time.Duration.Companion.milliseconds

class EnterTotpViewModel(
    hasOtherProvider: Boolean,
    private val isSetupFlow: Boolean,
    private val authToken: String?,
    private val signOutUseCase: SignOutUseCase,
    private val verifyTotpUseCase: VerifyTotpUseCase,
    private val oneTimeSessionRefreshRetry: OneTimeSessionRefreshRetry,
) : SideEffectViewModel<EnterTotpState, EnterTotpSideEffect>(
        EnterTotpState(hasOtherProvider = hasOtherProvider),
    ) {
    fun onIntent(intent: EnterTotpIntent) {
        when (intent) {
            is PasteFromClipboard -> emitSideEffect(PasteOtp)
            is ChooseOtherProvider -> emitSideEffect(NotifyChooseOtherProvider(authToken))
            is ToggleRememberMe -> updateViewState { copy(rememberMe = intent.checked) }
            is ValidateOtp -> validateOtp(intent.otp)
            is Close -> close()
            is ConfirmSetupLeave -> {
                updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
                signOutAndClose()
            }
            is DismissSetupLeave -> updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
        }
    }

    private fun close() {
        if (isSetupFlow) {
            updateViewState { copy(showSetupLeaveConfirmationDialog = true) }
        } else {
            signOutAndClose()
        }
    }

    private fun validateOtp(otp: String) {
        Timber.d("Verifying TOTP")
        updateViewState { copy(showProgress = true) }
        launch {
            val result =
                oneTimeSessionRefreshRetry.execute(
                    token = authToken.orEmpty(),
                    isUnauthorized = { it is Unauthorized },
                ) { bearer ->
                    verifyTotpUseCase.execute(VerifyTotpUseCase.Input(otp, bearer, viewState.value.rememberMe))
                }
            when (result) {
                is Failure -> genericError()
                is NetworkFailure -> networkError()
                is Success -> otpSuccess(result.mfaHeader)
                is WrongCode -> totpError()
                is Unauthorized -> {
                    emitSideEffect(ShowErrorSnackbar(SESSION_EXPIRED))
                    emitSideEffect(NavigateToLogin)
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

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
            delay(CLEAR_INPUT_DELAY)
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
        private val CLEAR_INPUT_DELAY = 1000L.milliseconds
    }
}
