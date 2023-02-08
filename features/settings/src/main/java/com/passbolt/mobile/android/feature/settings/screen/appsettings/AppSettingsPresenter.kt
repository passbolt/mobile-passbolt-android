package com.passbolt.mobile.android.feature.settings.screen.appsettings

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
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
class AppSettingsPresenter(
    private val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase,
    private val fingerprintInformationProvider: FingerprintInformationProvider,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val biometricCipher: BiometricCipher,
    private val biometryInteractor: BiometryInteractor,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : AppSettingsContract.Presenter {

    override var view: AppSettingsContract.View? = null

    override fun viewResumed() {
        handleFingerprintSwitchState()
    }

    private fun handleFingerprintSwitchState() {
        if (checkIfPassphraseExistsUseCase.execute(
                UserIdInput(requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount))
            ).passphraseFileExists
        ) {
            view?.toggleFingerprintOn(silently = true)
        } else {
            view?.toggleFingerprintOff(silently = true)
        }
    }

    override fun fingerprintSettingChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            view?.showDisableFingerprintConfirmationDialog()
        } else {
            if (fingerprintInformationProvider.hasBiometricSetUp()) {
                if (passphraseMemoryCache.hasPassphrase()) {
                    getPassphraseSucceeded()
                } else {
                    view?.navigateToAuthGetPassphrase()
                }
            } else {
                view?.toggleFingerprintOff(silently = true)
                view?.showConfigureFingerprintFirst()
            }
        }
    }

    override fun getPassphraseSucceeded() {
        tryShowingBiometricPrompt()
    }

    private fun tryShowingBiometricPrompt() {
        try {
            view?.showBiometricPrompt(biometricCipher.getBiometricEncryptCipher())
        } catch (exception: KeyPermanentlyInvalidatedException) {
            Timber.e(exception)
            biometryInteractor.disableBiometry()
            view?.showKeyChangesDetected()
        } catch (exception: Exception) {
            Timber.e(exception)
            view?.showBiometryError(exception.message)
        }
    }

    override fun biometricAuthError(errorMessage: Int) {
        view?.toggleFingerprintOff(silently = true)
        view?.showAuthenticationError(errorMessage)
    }

    override fun biometricAuthSucceeded(authenticatedCipher: Cipher?) {
        val passphrase = passphraseMemoryCache.get()
        if (passphrase is PotentialPassphrase.Passphrase && authenticatedCipher != null) {
            savePassphraseUseCase.execute(
                SavePassphraseUseCase.Input(passphrase.passphrase, authenticatedCipher)
            )
            saveBiometricKeyIvUseCase.execute(
                SaveBiometricKeyIvUseCase.Input(authenticatedCipher.iv)
            )
            view?.toggleFingerprintOn(silently = true)
        } else {
            Timber.e("Error during turing biometrics on. Passphrase not in cache after auth.")
        }
    }

    override fun biometricAuthCanceled() {
        view?.toggleFingerprintOff(silently = true)
    }

    override fun disableFingerprintConfirmed() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        removePassphraseUseCase.execute(UserIdInput(requireNotNull(selectedAccount)))
        view?.toggleFingerprintOff(silently = true)
    }

    override fun disableFingerprintCanceled() {
        view?.toggleFingerprintOn(silently = true)
    }

    override fun keyChangesInfoConfirmClick() {
        view?.navigateToAuthGetPassphrase()
    }

    override fun systemSettingsClick() {
        view?.navigateToSystemSettings()
    }

    override fun autofillClick() {
        view?.navigateToAutofillSettings()
    }

    override fun defaultFilterClick() {
        view?.navigateToDefaultFilterSettings(
            getHomeDisplayViewPrefsUseCase.execute(Unit).userSetHomeView
        )
    }
}
