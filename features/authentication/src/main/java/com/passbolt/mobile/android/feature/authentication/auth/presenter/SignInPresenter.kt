package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatus
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider.Companion.MFA_PROVIDER_TOTP
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider.Companion.MFA_PROVIDER_YUBIKEY
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInFailureType
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.session.SaveSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.crypto.Cipher
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

@Suppress("LongParameterList") // TODO extract interactors
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
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val featureFlagsInteractor: FeatureFlagsInteractor,
    private val signOutUseCase: SignOutUseCase,
    private val saveServerFingerprintUseCase: SaveServerFingerprintUseCase,
    private val isServerFingerprintCorrectUseCase: IsServerFingerprintCorrectUseCase,
    private val mfaStatusProvider: MfaStatusProvider,
    removeSelectedAccountPassphraseUseCase: RemoveSelectedAccountPassphraseUseCase,
    biometricCipher: BiometricCipher,
    getPassphraseUseCase: GetPassphraseUseCase,
    removeBiometricKeyUseCase: RemoveBiometricKeyUseCase,
    getPrivateKeyUseCase: GetPrivateKeyUseCase,
    verifyPassphraseUseCase: VerifyPassphraseUseCase,
    fingerprintInfoProvider: FingerprintInformationProvider,
    checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
    authReasonMapper: AuthReasonMapper
) : AuthBasePresenter(
    getAccountDataUseCase,
    checkIfPassphraseFileExistsUseCase,
    fingerprintInfoProvider,
    removeSelectedAccountPassphraseUseCase,
    getPrivateKeyUseCase,
    verifyPassphraseUseCase,
    biometricCipher,
    getPassphraseUseCase,
    passphraseMemoryCache,
    removeBiometricKeyUseCase,
    authReasonMapper,
    coroutineLaunchContext
) {

    private var loginState: LoginState? = null

    override fun onPassphraseVerified(passphrase: ByteArray) {
        performSignIn(passphrase)
    }

    override fun biometricAuthSuccess(authenticatedCipher: Cipher?) {
        super.biometricAuthSuccess(authenticatedCipher)
        when (val potentialPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                performSignIn(potentialPassphrase.passphrase)
            }
            else -> {
                view?.showGenericError()
                Timber.e("Passphrase not found in cache")
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
                val input = IsServerFingerprintCorrectUseCase.Input(userId, pgpKeyResult.fingerprint)
                if (!isServerFingerprintCorrectUseCase.execute(input).isCorrect) {
                    view?.hideProgress()
                    view?.showServerFingerprintChanged(pgpKeyResult.fingerprint)
                } else {
                    signIn(
                        passphrase.copyOf(),
                        pgpKeyResult.publicKey,
                        rsaKeyResult.rsaKey,
                        pgpKeyResult.fingerprint
                    )
                }
            } else {
                showGenericError()
            }
        }
    }

    private suspend fun signIn(
        passphrase: ByteArray,
        serverPublicKey: String,
        rsaKey: String,
        fingerprint: String
    ) {
        // TODO verify passphrase use case? Use stored url; PAS-214
        val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
        val challenge = challengeProvider.get(
            version = CHALLENGE_VERSION,
            domain = accountData.url,
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
                requireNotNull(accountData.serverId),
                fingerprint,
                accountData
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
        serverId: String,
        fingerprint: String,
        accountData: GetAccountDataUseCase.Output
    ) {
        when (val result = signInUseCase.execute(SiginInUseCase.Input(serverId, challenge))) {
            is SiginInUseCase.Output.Failure -> {
                view?.hideProgress()
                when (result.type) {
                    SignInFailureType.ACCOUNT_DOES_NOT_EXIST -> {
                        view?.showAccountDoesNotExistDialog(
                            "${accountData.firstName} ${accountData.lastName}",
                            accountData.email,
                            accountData.url
                        )
                    }
                    SignInFailureType.OTHER -> {
                        view?.showError(result.message)
                    }
                }
            }
            is SiginInUseCase.Output.Success -> {
                val challengeDecryptResult = challengeDecryptor.decrypt(
                    serverPublicKey,
                    passphrase,
                    userId,
                    result.challenge
                )
                when (challengeDecryptResult) {
                    is ChallengeDecryptor.Output.DecryptedChallenge ->
                        verifyChallenge(
                            challengeDecryptResult.challenge,
                            rsaKey,
                            passphrase,
                            fingerprint,
                            result.mfaToken
                        )
                    is ChallengeDecryptor.Output.DecryptionError -> {
                        view?.apply {
                            hideProgress()
                            showDecryptionError(challengeDecryptResult.message)
                        }
                    }
                }
            }
        }
    }

    override fun fingerprintServerConfirmationClick(fingerprint: String) {
        saveServerFingerprintUseCase.execute(SaveServerFingerprintUseCase.Input(userId, fingerprint))
    }

    private fun verifyChallenge(
        challengeResponseDto: ChallengeResponseDto,
        rsaKey: String,
        passphrase: ByteArray,
        fingerprint: String,
        mfaToken: String?
    ) {
        when (val result = challengeVerifier.verify(challengeResponseDto, rsaKey)) {
            ChallengeVerifier.Output.Failure -> showGenericError()
            ChallengeVerifier.Output.InvalidSignature -> showGenericError()
            ChallengeVerifier.Output.TokenExpired -> showGenericError()
            is ChallengeVerifier.Output.Verified -> {
                loginState = LoginState(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    passphrase = passphrase,
                    fingerprint = fingerprint,
                    mfaToken = mfaToken
                )
                when (val mfaStatus = mfaStatusProvider.provideMfaStatus(challengeResponseDto, mfaToken)) {
                    MfaStatus.NotRequired -> mfaNotRequired()
                    is MfaStatus.Required -> mfaRequired(mfaStatus.mfaProviders, result.accessToken)
                }
            }
        }
    }

    private fun mfaNotRequired() {
        signInSuccess()
    }

    private fun mfaRequired(mfaProviders: List<String>, jwtToken: String) {
        when (val provider = mfaProviders.first()) {
            MFA_PROVIDER_TOTP -> view?.showTotpDialog(jwtToken)
            MFA_PROVIDER_YUBIKEY -> view?.showYubikeyDialog(jwtToken)
            else -> {
                view?.showGenericError()
                Timber.e("Unknown provider: $provider")
            }
        }
    }

    override fun totpSucceeded(mfaHeader: String) {
        loginState?.mfaToken = mfaHeader
        signInSuccess()
    }

    private fun signInSuccess() {
        val currentLoginState = requireNotNull(loginState)
        saveSessionUseCase.execute(
            SaveSessionUseCase.Input(
                userId = userId,
                accessToken = currentLoginState.accessToken,
                refreshToken = currentLoginState.refreshToken,
                mfaToken = loginState?.mfaToken
            )
        )
        saveSelectedAccountUseCase.execute(UserIdInput(userId))
        passphraseMemoryCache.set(currentLoginState.passphrase)
        saveServerFingerprintUseCase.execute(SaveServerFingerprintUseCase.Input(currentLoginState.fingerprint, userId))
        currentLoginState.passphrase.erase()
        loginState?.passphrase?.erase()
        loginState = null
        fetchFeatureFlags()
    }

    private fun fetchFeatureFlags() {
        scope.launch {
            when (featureFlagsInteractor.fetchAndSaveFeatureFlags()) {
                is FeatureFlagsInteractor.Output.Success -> {
                    view?.apply {
                        hideProgress()
                        clearPassphraseInput()
                        authSuccess()
                    }
                }
                is FeatureFlagsInteractor.Output.Failure -> {
                    view?.showFeatureFlagsErrorDialog()
                }
            }
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

    fun signOutClick() {
        view?.apply {
            closeFeatureFlagsFetchErrorDialog()
            showProgress()
        }
        scope.launch {
            signOutUseCase.execute(Unit)
            view?.apply {
                hideProgress()
                navigateBack()
            }
        }
    }

    fun refreshClick() {
        when (val potentialPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> performSignIn(potentialPassphrase.passphrase)
            is PotentialPassphrase.PassphraseNotPresent -> view?.closeFeatureFlagsFetchErrorDialog()
        }
    }

    private class LoginState(
        val accessToken: String,
        val refreshToken: String,
        val passphrase: ByteArray,
        val fingerprint: String,
        var mfaToken: String? = null
    )

    private companion object {
        private const val CHALLENGE_VERSION = "1.0.0"
    }
}
