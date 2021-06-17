package com.passbolt.mobile.android.feature.login.login

import com.passbolt.mobile.android.common.extension.toByteArray
import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
class LoginPresenter(
    private val getServerPublicPgpKeyUseCase: GetServerPublicPgpKeyUseCase,
    private val getServerPublicRsaKeyUseCase: GetServerPublicRsaKeyUseCase,
    private val loginUseCase: LoginUseCase,
    private val challengeProvider: ChallengeProvider,
    coroutineLaunchContext: CoroutineLaunchContext
) : LoginContract.Presenter {

    override var view: LoginContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun signInClick(passphrase: CharArray?) {
        scope.launch {
            val pgpKey = async { getServerPublicPgpKeyUseCase.execute(Unit) }
            val rsaKey = async { getServerPublicRsaKeyUseCase.execute(Unit) }

            val pgpKeyResult = pgpKey.await()
            if (pgpKeyResult is GetServerPublicPgpKeyUseCase.Output.Success) {
                login(passphrase, pgpKeyResult.publicKey)
            } else {
                view?.showError()
            }
        }
    }

    private suspend fun login(passphrase: CharArray?, serverPublicKey: String) {
        // TODO data should be passed from the accounts list screen
        val userId = "e1ebc592-b90d-5e22-9f40-50e52911673b"
        val challenge = challengeProvider.get(
            version = "1.0.0",
            domain = "https://passbolt.dev",
            serverPublicKey = serverPublicKey,
            passphrase = requireNotNull(passphrase.toByteArray()),
            userId + "_passbolt.dev"
        )
        when (challenge) {
            is ChallengeProvider.Output.Success -> sendLoginRequest(userId, challenge.challenge)
            ChallengeProvider.Output.WrongPassphrase -> view?.showWrongPassphrase()
        }
    }

    private suspend fun sendLoginRequest(userId: String, challenge: String) {
        loginUseCase.execute(
            LoginUseCase.Input(
                userId,
                challenge
            )
        )
    }
}
