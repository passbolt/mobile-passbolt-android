package com.passbolt.mobile.android.feature.setup.fingerprint

import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.repository.passphrase.PassphraseRepository

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
    private val passphraseRepository: PassphraseRepository
) : FingerprintContract.Presenter {

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
            view?.showBiometricPrompt()
        } else {
            view?.navigateToBiometricSettings()
        }
    }

    override fun maybeLaterClick() {
        handleAutofillSetup()
    }

    override fun authenticationSucceeded() {
        handleAutofillSetup()
    }

    private fun handleAutofillSetup() {
        when (val cachedPassphrase = passphraseRepository.getPotentialPassphrase()) {
            is PotentialPassphrase.Passphrase -> {
                // fingerprint is good and passphrase in cache not expired - renew cache duration
                passphraseRepository.setPassphrase(cachedPassphrase.passphrase)
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
        view?.navigateToSignIn()
    }

    override fun goToTheAppClick() {
        view?.navigateToSignIn()
    }

    override fun autofillSetupSuccessfully() {
        view?.showAutofillEnabledDialog()
    }
}
