package com.passbolt.mobile.android.feature.setup.fingerprint

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.AuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationCancel
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationError
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.ConfirmKeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.DismissKeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.GoToApp
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.KeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.MaybeLater
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.ResumeView
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.UseFingerprint
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToAppSystemSettings
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToEncourageAutofill
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToHome
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowBiometricPrompt
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.StartAuthActivity
import com.passbolt.mobile.android.ui.BiometricAuthError
import com.passbolt.mobile.android.ui.BiometricAuthError.ERROR_LOCKOUT
import com.passbolt.mobile.android.ui.BiometricAuthError.ERROR_LOCKOUT_PERMANENT
import com.passbolt.mobile.android.ui.BiometricAuthError.GENERIC
import timber.log.Timber
import javax.crypto.Cipher

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

class FingerprintSetupViewModel(
    private val fingerprintInformationProvider: FingerprintInformationProvider,
    private val autofillInformationProvider: AutofillInformationProvider,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val biometryInteractor: BiometryInteractor,
) : SideEffectViewModel<FingerprintSetupState, FingerprintSetupSideEffect>(FingerprintSetupState()) {
    fun onIntent(intent: FingerprintSetupIntent) {
        when (intent) {
            ResumeView ->
                updateViewState {
                    copy(hasBiometricSetup = fingerprintInformationProvider.hasBiometricSetUp())
                }
            UseFingerprint -> useFingerprint()
            MaybeLater -> saveAccountData()
            is KeyPermanentlyInvalidated -> {
                Timber.e(intent.exception)
                biometryInteractor.disableBiometry()
                updateViewState { copy(showKeyChangesDetected = true) }
            }
            DismissKeyPermanentlyInvalidated -> updateViewState { copy(showKeyChangesDetected = false) }
            ConfirmKeyPermanentlyInvalidated -> emitSideEffect(StartAuthActivity)
            GoToApp -> emitSideEffect(NavigateToHome)
            AuthenticationSuccess -> showBiometricPrompt()
            is BiometricAuthenticationSuccess -> saveAccountData(intent.cipher)
            BiometricAuthenticationCancel -> {}
            is BiometricAuthenticationError -> biometricAuthenticationError(intent.error)
        }
    }

    private fun useFingerprint() {
        if (fingerprintInformationProvider.hasBiometricSetUp()) {
            showBiometricPrompt()
        } else {
            emitSideEffect(NavigateToAppSystemSettings)
        }
    }

    private fun showBiometricPrompt() {
        val cipher = biometricCipher.getBiometricEncryptCipher()
        emitSideEffect(ShowBiometricPrompt(cipher))
    }

    private fun saveAccountData(authenticatedCipher: Cipher? = null) {
        when (val cachedPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                authenticatedCipher?.let {
                    savePassphraseUseCase.execute(
                        SavePassphraseUseCase.Input(cachedPassphrase.passphrase, it),
                    )
                    saveBiometricKeyIvUseCase.execute(
                        SaveBiometricKeyIvUseCase.Input(authenticatedCipher.iv),
                    )
                }
                if (autofillInformationProvider.isAutofillServiceSupported() &&
                    !autofillInformationProvider.isPassboltAutofillServiceSet()
                ) {
                    emitSideEffect(NavigateToEncourageAutofill)
                } else {
                    emitSideEffect(NavigateToHome)
                }
            }
            is PotentialPassphrase.PassphraseNotPresent -> {
                emitSideEffect(StartAuthActivity)
            }
        }
    }

    private fun biometricAuthenticationError(error: BiometricAuthError) {
        val errorType =
            when (error) {
                ERROR_LOCKOUT -> SnackbarErrorType.AUTHENTICATION_LOCKOUT
                ERROR_LOCKOUT_PERMANENT -> SnackbarErrorType.AUTHENTICATION_LOCKOUT_PERMANENT
                GENERIC -> SnackbarErrorType.AUTHENTICATION_GENERIC
            }
        emitSideEffect(ShowErrorSnackbar(errorType))
    }
}
