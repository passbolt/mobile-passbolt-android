package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.repository.passphrase.PassphraseRepository
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.session.SaveSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val saveSessionUseCase: SaveSessionUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val passphraseRepository: PassphraseRepository,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removeSelectedAccountPassphraseUseCase: RemoveSelectedAccountPassphraseUseCase,
    fingerprintInfoProvider: FingerprintInformationProvider,
    checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase,
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
        performSignIn(passphrase)
    }

    override fun biometricAuthSuccess() {
        val potentialPassphrase = passphraseRepository.getCaching()
        when {
            potentialPassphrase is PotentialPassphrase.Passphrase -> {
                performSignIn(potentialPassphrase.passphrase)
            }
            potentialPassphrase is PotentialPassphrase.PassphraseNotPresent &&
                    potentialPassphrase.keyStatus == PotentialPassphrase.KeyStatus.INVALID -> {
                removeSelectedAccountPassphraseUseCase.execute(Unit)
                view?.setBiometricAuthButtonGone()
                view?.showFingerprintChangedError()
            }
            else -> {
                view?.showGenericError()
                Timber.e("Passphrase not found in repository")
            }
        }
    }

    private fun performSignIn(passphrase: ByteArray) {
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

    private suspend fun signIn(passphrase: ByteArray, serverPublicKey: String, rsaKey: String) {
        // TODO verify passphrase use case? Use stored url; PAS-214
        val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
        val challenge = challengeProvider.get(
            version = "1.0.0",
            domain = "https://passbolt.dev",
            serverPublicKey = serverPublicKey,
            passphrase = passphrase,
            userId
        )
        when (challenge) {
            is ChallengeProvider.Output.Success -> sendSignInRequest(
                userId,
                challenge.challenge,
                serverPublicKey,
                passphrase,
                rsaKey,
                requireNotNull(accountData.serverId)
            )
            ChallengeProvider.Output.WrongPassphrase -> showWrongPassphrase()
        }
    }

    private suspend fun sendSignInRequest(
        userId: String,
        challenge: String,
        serverPublicKey: String,
        passphrase: ByteArray,
        rsaKey: String,
        serverId: String
    ) {
        when (val result = signInUseCase.execute(SiginInUseCase.Input(serverId, challenge))) {
            is SiginInUseCase.Output.Failure -> {
                view?.showError(result.message)
                view?.hideProgress()
            }
            is SiginInUseCase.Output.Success -> {
                val challengeDecryptResult = challengeDecryptor.decrypt(
                    serverPublicKey,
                    passphrase,
                    userId,
                    result.challenge
                )
                verifyChallenge(challengeDecryptResult, rsaKey, userId, passphrase)
            }
        }
    }

    private fun verifyChallenge(
        challengeResponseDto: ChallengeResponseDto,
        rsaKey: String,
        userId: String,
        passphrase: ByteArray
    ) {
        when (val result = challengeVerifier.verify(challengeResponseDto, rsaKey)) {
            ChallengeVerifier.Output.Failure -> showGenericError()
            ChallengeVerifier.Output.InvalidSignature -> showGenericError()
            ChallengeVerifier.Output.TokenExpired -> showGenericError()
            is ChallengeVerifier.Output.Verified -> signInSuccess(result.accessToken, userId, passphrase)
        }
    }

    private fun signInSuccess(accessToken: String, userId: String, passphrase: ByteArray) {
        saveSessionUseCase.execute(
            SaveSessionUseCase.Input(
                userId = userId,
                accessToken = accessToken,
                refreshToken = ""
            )
        )
        saveSelectedAccountUseCase.execute(UserIdInput(userId))
        passphraseMemoryCache.set(passphrase)
        passphrase.erase()
        view?.apply {
            hideProgress()
            clearPassphraseInput()
            authSuccess()
        }
    }

    private fun showGenericError() {
        view?.apply {
            hideProgress()
            showGenericError()
        }
    }

    private fun showWrongPassphrase() {
        view?.apply {
            hideProgress()
            showWrongPassphrase()
        }
    }
}
