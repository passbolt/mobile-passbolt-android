package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.usecase.account.SaveAccountUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
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
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val verifyPassphraseUseCase: VerifyPassphraseUseCase,
    private val saveAccountUseCase: SaveAccountUseCase,
    fingerprintInfoProvider: FingerprintInformationProvider,
    removeSelectedAccountPassphraseUseCase: RemoveSelectedAccountPassphraseUseCase,
    checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    getAccountDataUseCase: GetAccountDataUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthBasePresenter(
    getAccountDataUseCase,
    checkIfPassphraseFileExistsUseCase,
    fingerprintInfoProvider,
    removeSelectedAccountPassphraseUseCase,
    coroutineLaunchContext
) {

    override fun signInClick(passphrase: ByteArray) {
        super.signInClick(passphrase)
        validatePassphrase(passphrase)
    }

    override fun biometricAuthSuccess() {
        view?.authSuccess()
    }

    private fun validatePassphrase(passphrase: ByteArray) {
        scope.launch {
            val privateKey = requireNotNull(getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey)
            val isCorrect =
                verifyPassphraseUseCase.execute(VerifyPassphraseUseCase.Input(privateKey, passphrase)).isCorrect
            if (!isCorrect) {
                view?.showWrongPassphrase()
            } else {
                saveAccountUseCase.execute(UserIdInput(userId))
                passphraseMemoryCache.set(passphrase)
                passphrase.erase()
                view?.apply {
                    clearPassphraseInput()
                    authSuccess()
                }
            }
        }
    }
}