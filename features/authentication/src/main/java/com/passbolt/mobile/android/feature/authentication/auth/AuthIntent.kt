package com.passbolt.mobile.android.feature.authentication.auth

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.ui.BiometricAuthError
import javax.crypto.Cipher

sealed interface AuthIntent {
    data object GoBack : AuthIntent

    data object SignIn : AuthIntent

    class PassphraseInputChanged(
        val passphrase: ByteArray,
    ) : AuthIntent

    data object AuthenticateUsingBiometry : AuthIntent

    data class BiometricAuthenticationSuccess(
        val cipher: Cipher?,
    ) : AuthIntent

    data class BiometricAuthenticationError(
        val error: BiometricAuthError,
    ) : AuthIntent

    data object BiometricKeyInvalidated : AuthIntent

    data object ForgotPassword : AuthIntent

    data object ConfirmSetupLeave : AuthIntent

    data object DismissConfirmSetupLeave : AuthIntent

    data object OpenHelpMenu : AuthIntent

    data object DismissHelpMenu : AuthIntent

    data object AccessLogs : AuthIntent

    data object ConnectToExistingAccount : AuthIntent

    data object DismissNoAccountExplanation : AuthIntent

    data object RootedDeviceAcknowledged : AuthIntent

    data class AcceptChangedServerFingerprint(
        val fingerprint: String,
    ) : AuthIntent

    data class MfaSucceeded(
        val mfaHeader: String?,
    ) : AuthIntent

    data class ChooseOtherMfaProvider(
        val bearer: String?,
        val currentProvider: MfaProvider,
    ) : AuthIntent

    data object Retry : AuthIntent

    data object SignOut : AuthIntent

    data object DismissServerNotReachable : AuthIntent

    data object RejectChangedServerFingerprint : AuthIntent
}
