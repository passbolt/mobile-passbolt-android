package com.passbolt.mobile.android.feature.setup.fingerprint

import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.storage.usecase.SavePassphraseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    private val fingerprintProvider: FingerprintInformationProvider,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : FingerprintContract.Presenter {

    override var view: FingerprintContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun resume() {
        if (fingerprintProvider.hasBiometricSetUp()) {
            view?.showUseFingerprint()
        } else {
            view?.showConfigureFingerprint()
        }
    }

    override fun useFingerprintButtonClick() {
        if (fingerprintProvider.hasBiometricSetUp()) {
            view?.showBiometricPrompt()
        } else {
            view?.navigateToBiometricSettings()
        }
    }

    override fun authenticationSucceeded() {
        scope.launch {
            // TODO - save proper passphrase PAS-125
            savePassphraseUseCase.execute(SavePassphraseUseCase.Input("pass".toCharArray()))
        }
    }

    override fun authenticationError(error: String) {
        view?.showAuthenticationError()
    }

    override fun authenticationFailed() {
        view?.showAuthenticationError()
    }
}
