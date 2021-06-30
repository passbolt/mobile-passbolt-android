package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.extension.toByteArray
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.storage.usecase.GetAccountDataUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase.Output.Success as PgpSuccess
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase.Output.Success as RsaSuccess

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
class SignInPresenter(
    private val getServerPublicPgpKeyUseCase: GetServerPublicPgpKeyUseCase,
    private val getServerPublicRsaKeyUseCase: GetServerPublicRsaKeyUseCase,
    private val signInUseCase: SiginInUseCase,
    private val challengeProvider: ChallengeProvider,
    private val challengeDecryptor: ChallengeDecryptor,
    private val challengeVerifier: ChallengeVerifier,
    getAccountDataUseCase: GetAccountDataUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthBasePresenter(getAccountDataUseCase, coroutineLaunchContext) {

    override fun signInClick(passphrase: CharArray?) {
        super.signInClick(passphrase)
        view?.showProgress()
        scope.launch {
            val pgpKey = async { getServerPublicPgpKeyUseCase.execute(Unit) }
            val rsaKey = async { getServerPublicRsaKeyUseCase.execute(Unit) }

            val pgpKeyResult = pgpKey.await()
            val rsaKeyResult = rsaKey.await()
            if (pgpKeyResult is PgpSuccess && rsaKeyResult is RsaSuccess) {
                signIn(passphrase, pgpKeyResult.publicKey, rsaKeyResult.rsaKey)
            } else {
                showGenericError()
            }
        }
    }

    private suspend fun signIn(passphrase: CharArray?, serverPublicKey: String, rsaKey: String) {
        // TODO verify passphrase use case?
        // TODO refactor using local user id and server user id
        val challenge = challengeProvider.get(
            version = "1.0.0",
            domain = "https://passbolt.dev",
            serverPublicKey = serverPublicKey,
            passphrase = requireNotNull(passphrase.toByteArray()),
            userId
        )
        when (challenge) {
            is ChallengeProvider.Output.Success -> sendSignInRequest(
                // TODO refactor using local user id and server user id
                userId.removeSuffix("_passbolt.dev"),
                challenge.challenge,
                serverPublicKey,
                requireNotNull(passphrase),
                rsaKey
            )
            ChallengeProvider.Output.WrongPassphrase -> showWrongPassphrase()
        }
    }

    private suspend fun sendSignInRequest(
        userId: String,
        challenge: String,
        serverPublicKey: String,
        passphrase: CharArray,
        rsaKey: String
    ) {
        when (val result = signInUseCase.execute(SiginInUseCase.Input(userId, challenge))) {
            is SiginInUseCase.Output.Failure -> {
                view?.showError(result.message)
                view?.hideProgress()
            }
            is SiginInUseCase.Output.Success -> {
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
            ChallengeVerifier.Output.Failure -> showGenericError()
            ChallengeVerifier.Output.InvalidSignature -> showGenericError()
            ChallengeVerifier.Output.TokenExpired -> showGenericError()
            is ChallengeVerifier.Output.Verified -> signInSuccess()
        }
    }

    private fun signInSuccess() {
        view?.hideProgress()
        view?.authSuccess()
    }

    private fun showGenericError() {
        view?.hideProgress()
        view?.showGenericError()
    }

    private fun showWrongPassphrase() {
        view?.hideProgress()
        view?.showWrongPassphrase()
    }
}
