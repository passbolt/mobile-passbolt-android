package com.passbolt.mobile.android.feature.authentication.auth

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import javax.crypto.Cipher

sealed interface AuthSideEffect {
    data object NavigateBack : AuthSideEffect

    data class AuthSuccess(
        val authConfig: ActivityIntents.AuthConfig,
        val appContext: AppContext,
    ) : AuthSideEffect

    data object NavigateToAccountList : AuthSideEffect

    data object NavigateToLogs : AuthSideEffect

    data class LaunchBiometricPrompt(
        val cipher: Cipher,
        val authReason: AuthState.RefreshAuthReason?,
    ) : AuthSideEffect

    data class ShowErrorSnackbar(
        val kind: SnackbarErrorType,
        val message: String? = null,
    ) : AuthSideEffect

    data object HideKeyboard : AuthSideEffect

    data object FinishAffinity : AuthSideEffect

    data class NavigateToMfa(
        val mfaState: MfaDialogState,
    ) : AuthSideEffect

    enum class SnackbarErrorType {
        WRONG_PASSPHRASE,
        GENERIC,
        CONNECTION_FAILURE,
        OFFLINE_DATA_EXPIRED,
        BIOMETRIC_CHANGED,
        AUTHENTICATION_ERROR,
        DECRYPTION_ERROR,
        CHALLENGE_INVALID_SIGNATURE,
        CHALLENGE_TOKEN_EXPIRED,
        CHALLENGE_VERIFICATION_FAILURE,
        TIME_OUT_OF_SYNC,
        BIOMETRIC_DECRYPT_ERROR,
        BIOMETRIC_NO_CRYPTO_CIPHER,
        BIOMETRIC_LOCKOUT,
        BIOMETRIC_LOCKOUT_PERMANENT,
    }
}
