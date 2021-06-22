package com.passbolt.mobile.android.feature.login.login

import com.passbolt.mobile.android.common.extension.toByteArray
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.login.login.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.login.login.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.login.login.challenge.ChallengeVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.passbolt.mobile.android.feature.login.login.GetServerPublicRsaKeyUseCase.Output.Success as RsaSuccess
import com.passbolt.mobile.android.feature.login.login.GetServerPublicPgpKeyUseCase.Output.Success as PgpSuccess

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
    private val challengeDecryptor: ChallengeDecryptor,
    private val challengeVerifier: ChallengeVerifier,
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
            val rsaKeyResult = rsaKey.await()
            if (pgpKeyResult is PgpSuccess && rsaKeyResult is RsaSuccess) {
                login(passphrase, pgpKeyResult.publicKey, rsaKeyResult.rsaKey)
            } else {
                view?.showError()
            }
        }
    }

    override fun backClick() {
        view?.navigateBack()
    }

    private suspend fun login(passphrase: CharArray?, serverPublicKey: String, rsaKey: String) {
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
            is ChallengeProvider.Output.Success -> sendLoginRequest(
                userId,
                challenge.challenge,
                serverPublicKey,
                requireNotNull(passphrase),
                rsaKey
            )
            ChallengeProvider.Output.WrongPassphrase -> view?.showWrongPassphrase()
        }
    }

    private suspend fun sendLoginRequest(
        userId: String,
        challenge: String,
        serverPublicKey: String,
        passphrase: CharArray,
        rsaKey: String
    ) {
        when (val result = loginUseCase.execute(LoginUseCase.Input(userId, challenge))) {
            LoginUseCase.Output.Failure -> {
                // TODO
            }
            is LoginUseCase.Output.Success -> {
                val challengeDecryptResult = challengeDecryptor.decrypt(
                    serverPublicKey,
                    passphrase.toByteArray()!!,
                    userId + "_passbolt.dev",
                    result.challenge
                )
                verifyChallenge(challengeDecryptResult, rsaKey)
            }
        }
    }

    private fun verifyChallenge(challengeResponseDto: ChallengeResponseDto, rsaKey: String) {
        when (challengeVerifier.verify(challengeResponseDto, rsaKey)) {
            ChallengeVerifier.Output.Failure -> {
                // TODO
            }
            ChallengeVerifier.Output.InvalidSignature -> {
                // TODO
            }
            ChallengeVerifier.Output.TokenExpired -> {
                // TODO
            }
            is ChallengeVerifier.Output.Verified -> {
                // TODO
            }
        }
    }
}
