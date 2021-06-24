package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.storage.repository.passphrase.PassphraseRepository
import com.passbolt.mobile.android.storage.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedUserPrivateKeyUseCase
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

// presenter for sign in view used for just obtaining the passphrase in the cache without performing API sign in
// handles storing passphrase in cache after sign in button clicked
class PassphrasePresenter(
    private val passphraseRepository: PassphraseRepository,
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val verifyPassphraseUseCase: VerifyPassphraseUseCase,
    getAccountDataUseCase: GetAccountDataUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthBasePresenter(getAccountDataUseCase, coroutineLaunchContext) {

    override fun signInClick(passphrase: CharArray?) {
        passphrase?.let {
            validatePassphrase(passphrase)
        }
    }

    private fun validatePassphrase(passphrase: CharArray) {
        scope.launch {
            val privateKey = requireNotNull(getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey)
            val isCorrect =
                verifyPassphraseUseCase.execute(VerifyPassphraseUseCase.Input(privateKey, passphrase)).isCorrect
            if (!isCorrect) {
                view?.showWrongPassphrase()
                passphraseRepository.clearPassphrase()
            } else {
                passphraseRepository.setPassphrase(passphrase)
                view?.authSuccess()
            }
        }
    }
}
