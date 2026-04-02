package com.passbolt.mobile.android.feature.authentication.auth

import android.content.Context
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.AUTHENTICATION_ERROR
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.BIOMETRIC_CHANGED
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.BIOMETRIC_DECRYPT_ERROR
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.BIOMETRIC_NO_CRYPTO_CIPHER
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.CHALLENGE_INVALID_SIGNATURE
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.CHALLENGE_TOKEN_EXPIRED
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.CHALLENGE_VERIFICATION_FAILURE
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.DECRYPTION_ERROR
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.PROFILE_FETCH_FAILURE
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.TIME_OUT_OF_SYNC
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.WRONG_PASSPHRASE
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getTitleText(
    context: Context,
    authReason: AuthState.RefreshAuthReason?,
): String =
    when (authReason) {
        null -> context.getString(LocalizationR.string.auth_sign_in)
        else -> context.getString(LocalizationR.string.auth_enter_passphrase)
    }

@Suppress("CyclomaticComplexMethod")
internal fun getSnackBarMessage(
    context: Context,
    kind: AuthSideEffect.SnackbarErrorType,
    message: String?,
): String =
    when (kind) {
        WRONG_PASSPHRASE -> context.getString(LocalizationR.string.auth_incorrect_passphrase)
        GENERIC -> context.getString(LocalizationR.string.common_failure)
        BIOMETRIC_CHANGED -> context.getString(LocalizationR.string.biometric_changed_title)
        AUTHENTICATION_ERROR -> message ?: context.getString(LocalizationR.string.common_failure)
        DECRYPTION_ERROR -> {
            val base = context.getString(LocalizationR.string.auth_decryption_error_description)
            if (!message.isNullOrBlank()) {
                base + context.getString(LocalizationR.string.auth_decryption_error_cause, message)
            } else {
                base
            }
        }
        CHALLENGE_INVALID_SIGNATURE -> context.getString(LocalizationR.string.auth_error_invalid_signature)
        CHALLENGE_TOKEN_EXPIRED -> context.getString(LocalizationR.string.auth_error_token_expired)
        CHALLENGE_VERIFICATION_FAILURE -> context.getString(LocalizationR.string.auth_error_challenge_verification_failure)
        TIME_OUT_OF_SYNC -> context.getString(LocalizationR.string.common_time_is_out_of_sync)
        PROFILE_FETCH_FAILURE -> {
            val base = context.getString(LocalizationR.string.auth_error_profile_fetch_failure)
            if (!message.isNullOrBlank()) {
                "$base($message)"
            } else {
                base
            }
        }
        BIOMETRIC_DECRYPT_ERROR -> context.getString(LocalizationR.string.biometric_decrypt_error_message)
        BIOMETRIC_NO_CRYPTO_CIPHER -> context.getString(LocalizationR.string.biometric_no_crypto_cipher)
    }
