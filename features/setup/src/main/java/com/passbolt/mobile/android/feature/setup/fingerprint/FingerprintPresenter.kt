package com.passbolt.mobile.android.feature.setup.fingerprint

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import org.koin.core.component.KoinComponent
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
class FingerprintPresenter(
    private val fingerprintInformationProvider: FingerprintInformationProvider,
    private val autofillInformationProvider: AutofillInformationProvider,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val removeBiometricKeyUseCase: RemoveBiometricKeyUseCase
) : FingerprintContract.Presenter, KoinComponent {

    override var view: FingerprintContract.View? = null

    override fun resume() {
        if (fingerprintInformationProvider.hasBiometricSetUp()) {
            view?.showUseFingerprint()
        } else {
            view?.showConfigureFingerprint()
        }
    }

    override fun useFingerprintClick() {
        if (fingerprintInformationProvider.hasBiometricSetUp()) {
            tryShowingBiometricPrompt()
        } else {
            view?.navigateToSystemSettings()
        }
    }

    private fun tryShowingBiometricPrompt() {
        try {
            view?.showBiometricPrompt(biometricCipher.getBiometricEncryptCipher())
        } catch (exception: KeyPermanentlyInvalidatedException) {
            Timber.e(exception)
            removeBiometricKeyUseCase.execute(Unit)
            view?.showKeyChangesDetected()
        } catch (exception: Exception) {
            Timber.e(exception)
            view?.showGenericError()
        }
    }

    override fun keyChangesInfoConfirmClick() {
        view?.startAuthActivity()
    }

    override fun maybeLaterClick() {
        handleAutofillSetup()
    }

    override fun getPassphraseSucceeded() {
        tryShowingBiometricPrompt()
    }

    override fun authenticationSucceeded(authenticatedCipher: Cipher?) {
        handleAutofillSetup(authenticatedCipher)
    }

    private fun handleAutofillSetup(authenticatedCipher: Cipher? = null) {
        when (val cachedPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                authenticatedCipher?.let {
                    savePassphraseUseCase.execute(
                        SavePassphraseUseCase.Input(cachedPassphrase.passphrase, it)
                    )
                    saveBiometricKeyIvUseCase.execute(
                        SaveBiometricKeyIvUseCase.Input(authenticatedCipher.iv)
                    )
                }
                if (!autofillInformationProvider.isPassboltAutofillServiceSet()) {
                    view?.showEncourageAutofillDialog()
                } else {
                    view?.showAutofillEnabledDialog()
                }
            }
            is PotentialPassphrase.PassphraseNotPresent -> {
                // user stayed too long and passphrase cache expired - authenticate
                view?.startAuthActivity()
            }
        }
    }

    override fun authenticationError(errorMessage: Int) {
        view?.showAuthenticationError(errorMessage)
    }

    override fun setupAutofillLaterClick() {
        view?.navigateToHome()
    }

    override fun goToTheAppClick() {
        view?.navigateToHome()
    }

    override fun autofillDialogSuccess() {
        view?.showAutofillEnabledDialog()
    }
}
