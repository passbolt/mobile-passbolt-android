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

package com.passbolt.mobile.android.feature.settings.screen.appsettings

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfigureFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfirmKeyChange
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CancelDisableFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CanceledBiometricAuth
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfigureFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmDisableFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmKeyChangeClick
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ErroredBiometricAuth
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.FinalizedBiometricAuth
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoToAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoToDefaultFilter
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoToExpertSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.InvalidateBiometricKeyPermanently
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.RefreshedPassphrase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ShowBiometryError
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ToggleFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.LaunchBiometricPrompt
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToDefaultFilter
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToExpertSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToGetPassphrase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToSystemSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.SnackbarKind.AUTHENTICAION_ERROR
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.SnackbarKind.BIOMETRY_ERROR
import com.passbolt.mobile.android.ui.BiometricAuthError
import timber.log.Timber
import javax.crypto.Cipher

internal class AppSettingsViewModel(
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val fingerprintInformationProvider: FingerprintInformationProvider,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val biometryInteractor: BiometryInteractor,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
) : SideEffectViewModel<AppSettingsState, AppSettingsSideEffect>(AppSettingsState()) {
    init {
        loadInitialValues()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: AppSettingsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            GoToAutofill -> emitSideEffect(NavigateToAutofill)
            GoToDefaultFilter -> emitSideEffect(NavigateToDefaultFilter)
            GoToExpertSettings -> emitSideEffect(NavigateToExpertSettings)
            ToggleFingerprint -> toggleFingerprint()
            CancelDisableFingerprint -> updateViewState { copy(isDisableFingerprintDialogVisible = false) }
            ConfirmDisableFingerprint -> disableFingerprint()
            CancelConfigureFingerprint -> updateViewState { copy(isConfigureFingerprintDialogVisible = false) }
            ConfigureFingerprint -> {
                updateViewState { copy(isConfigureFingerprintDialogVisible = false) }
                emitSideEffect(NavigateToSystemSettings)
            }
            RefreshedPassphrase -> authenticateUsingBiometryPrompt()
            CanceledBiometricAuth -> {}
            is ErroredBiometricAuth -> biometricAuthError(intent.error)
            is FinalizedBiometricAuth -> finalizedBiometricAuth(intent.cipher)
            is InvalidateBiometricKeyPermanently -> invalidateBiometricKey(intent.exception)
            is ShowBiometryError -> biometryShowError(intent.exception)
            CancelConfirmKeyChange -> updateViewState { copy(isKeyChangesDialogDetectedVisible = false) }
            ConfirmKeyChangeClick -> {
                updateViewState { copy(isKeyChangesDialogDetectedVisible = false) }
                emitSideEffect(NavigateToGetPassphrase)
            }
        }
    }

    private fun biometryShowError(exception: Exception) {
        Timber.e(exception)
        emitSideEffect(
            AppSettingsSideEffect.ShowErrorSnackbar(
                snackbarKind = BIOMETRY_ERROR,
                exception.message,
            ),
        )
    }

    private fun invalidateBiometricKey(exception: KeyPermanentlyInvalidatedException) {
        Timber.e(exception)
        biometryInteractor.disableBiometry()
        updateViewState { copy(isKeyChangesDialogDetectedVisible = true) }
    }

    private fun biometricAuthError(error: BiometricAuthError) {
        Timber.e("Biometric authentication error: ${error.name}")
        emitSideEffect(
            AppSettingsSideEffect.ShowErrorSnackbar(
                snackbarKind = AUTHENTICAION_ERROR,
                error.name,
            ),
        )
    }

    private fun finalizedBiometricAuth(authenticatedCipher: Cipher?) {
        val passphrase = passphraseMemoryCache.get()
        if (passphrase is PotentialPassphrase.Passphrase && authenticatedCipher != null) {
            savePassphraseUseCase.execute(
                SavePassphraseUseCase.Input(
                    passphrase.passphrase,
                    authenticatedCipher,
                ),
            )
            saveBiometricKeyIvUseCase.execute(
                SaveBiometricKeyIvUseCase.Input(
                    authenticatedCipher.iv,
                ),
            )
            updateViewState { copy(isFingerprintEnabled = true) }
        } else {
            Timber.e("Error during turing biometrics on. Passphrase not in cache after auth.")
        }
    }

    private fun loadInitialValues() {
        val passphraseFileExists =
            checkIfPassphraseExistsUseCase
                .execute(
                    UserIdInput(requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)),
                ).passphraseFileExists
        updateViewState {
            copy(isFingerprintEnabled = passphraseFileExists)
        }
    }

    private fun toggleFingerprint() {
        val isEnabled = viewState.value.isFingerprintEnabled
        if (isEnabled) {
            updateViewState { copy(isDisableFingerprintDialogVisible = true) }
        } else {
            if (fingerprintInformationProvider.hasBiometricSetUp()) {
                if (passphraseMemoryCache.hasPassphrase()) {
                    authenticateUsingBiometryPrompt()
                } else {
                    emitSideEffect(NavigateToGetPassphrase)
                }
            } else {
                updateViewState { copy(isFingerprintEnabled = false, isConfigureFingerprintDialogVisible = true) }
            }
        }
    }

    fun authenticateUsingBiometryPrompt() {
        emitSideEffect(LaunchBiometricPrompt(biometricCipher.getBiometricEncryptCipher()))
    }

    fun disableFingerprint() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        removePassphraseUseCase.execute(UserIdInput(requireNotNull(selectedAccount)))
        updateViewState { copy(isDisableFingerprintDialogVisible = false, isFingerprintEnabled = false) }
    }
}
