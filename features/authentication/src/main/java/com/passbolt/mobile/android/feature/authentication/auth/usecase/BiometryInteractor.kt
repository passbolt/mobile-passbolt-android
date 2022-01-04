package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import timber.log.Timber

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
class BiometryInteractor(
    private val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val removeSelectedAccountPassphraseUseCase: RemoveSelectedAccountPassphraseUseCase,
    private val removeBiometricKeyUseCase: RemoveBiometricKeyUseCase,
    private val fingerprintInfoProvider: FingerprintInformationProvider
) {

    fun onBiometryReady(userId: String, onReady: () -> Unit) {
        Timber.d("Checking biometry state")
        if (checkIfPassphraseFileExistsUseCase.execute(UserIdInput(userId)).passphraseFileExists) {
            if (fingerprintInfoProvider.hasBiometricSetUp()) {
                Timber.d("Biometry ready")
                onReady()
            } else {
                Timber.d("Disabling biometry")
                disableBiometry()
            }
        }
    }

    fun disableBiometry() {
        removeSelectedAccountPassphraseUseCase.execute(Unit)
        removeBiometricKeyUseCase.execute(Unit)
    }
}
