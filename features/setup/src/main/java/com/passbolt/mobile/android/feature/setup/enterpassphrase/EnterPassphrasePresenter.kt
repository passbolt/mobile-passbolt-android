package com.passbolt.mobile.android.feature.setup.enterpassphrase

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase.Input
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintInformationProvider
import com.passbolt.mobile.android.storage.repository.passphrase.PassphraseRepository
import com.passbolt.mobile.android.storage.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.SaveUserAvatarUseCase
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
class EnterPassphrasePresenter(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val saveUserAvatarUseCase: SaveUserAvatarUseCase,
    private val fingerprintProvider: FingerprintInformationProvider,
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val verifyPassphraseUseCase: VerifyPassphraseUseCase,
    private val passphraseRepository: PassphraseRepository,
    coroutineLaunchContext: CoroutineLaunchContext
) : EnterPassphraseContract.Presenter {

    override var view: EnterPassphraseContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: EnterPassphraseContract.View) {
        super.attach(view)
        displayAccount()
    }

    private fun displayAccount() {
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount
        val accountData = getAccountDataUseCase.execute(GetAccountDataUseCase.Input(userId))
        accountData.avatarUrl?.let {
            view?.displayAvatar(it)
        }
        view?.displayName("${accountData.firstName} ${accountData.lastName}")
        view?.displayUrl(accountData.url)
        view?.displayEmail(accountData.email)
    }

    override fun passwordChanged(isEmpty: Boolean) {
        view?.setButtonEnabled(!isEmpty)
    }

    override fun onImageLoaded(image: ByteArray) {
        saveUserAvatarUseCase.execute(SaveUserAvatarUseCase.Input(image))
    }

    override fun forgotPasswordClick() {
        view?.showForgotPasswordDialog()
    }

    override fun singInClick(passphrase: CharArray) {
        validatePassphrase(passphrase)
    }

    private fun validatePassphrase(passphrase: CharArray) {
        scope.launch {
            val privateKey = requireNotNull(getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey)
            val isCorrect = verifyPassphraseUseCase.execute(Input(privateKey, passphrase)).isCorrect
            if (!isCorrect) {
                view?.showWrongPassphraseError()
                passphraseRepository.clearPassphrase()
            } else {
                passphraseRepository.setPassphrase(passphrase)
                navigateToNextScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
        if (fingerprintProvider.hasBiometricHardware()) {
            view?.navigateToBiometricSetup()
        } else {
            // TODO
        }
    }
}
