package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
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

typealias ChallengeVerificationErrorType = SignInVerifyInteractor.Error.ChallengeVerificationError.Type

class SignInVerifyInteractor(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val challengeProvider: ChallengeProvider,
    private val getSessionUseCase: GetSessionUseCase,
    private val signInUseCase: SignInUseCase,
    private val challengeDecryptor: ChallengeDecryptor,
    private val challengeVerifier: ChallengeVerifier,
    private val inAppReviewInteractor: InAppReviewInteractor
) {

    suspend fun signInVerify(
        serverPgpPublicKey: String,
        passphrase: ByteArray,
        userId: String,
        serverRsaKey: String,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit
    ) {
        Timber.d("Preparing sign in challenge")
        val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
        val challenge = challengeProvider.get(
            domain = accountData.url,
            serverPublicKey = serverPgpPublicKey,
            passphrase = passphrase,
            userId = userId
        )
        when (challenge) {
            is ChallengeProvider.Output.Success -> {
                Timber.d("Prepared sign in challenge")
                sendSignInRequest(
                    userId,
                    challenge.challenge,
                    serverPgpPublicKey,
                    passphrase,
                    serverRsaKey,
                    requireNotNull(accountData.serverId),
                    accountData,
                    onError,
                    onSuccess
                )
            }
            ChallengeProvider.Output.WrongPassphrase -> {
                Timber.d("Error during preparing challenge - incorrect passphrase")
                onError(Error.IncorrectPassphrase)
            }
        }
    }

    @Suppress("LongParameterList") // TODO extract member data class?
    private suspend fun sendSignInRequest(
        userId: String,
        challenge: String,
        serverPublicKey: String,
        passphrase: ByteArray,
        rsaKey: String,
        serverId: String,
        accountData: GetAccountDataUseCase.Output,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit
    ) {
        Timber.d("Signing in")
        val currentMfaToken = getSessionUseCase.execute(Unit).mfaToken
        when (val result = signInUseCase.execute(SignInUseCase.Input(serverId, challenge, currentMfaToken))) {
            is SignInUseCase.Output.Failure -> {
                Timber.e("Failure during sign in: ${result.message}")
                val error = when (result.type) {
                    SignInFailureType.ACCOUNT_DOES_NOT_EXIST -> {
                        Error.AccountDoesNotExist(
                            accountData.label ?: AccountModelMapper.defaultLabel(
                                accountData.firstName,
                                accountData.lastName
                            ),
                            accountData.email,
                            accountData.url
                        )
                    }
                    SignInFailureType.OTHER -> {
                        Error.SignInFailure(result.message)
                    }
                }
                onError(error)
            }
            is SignInUseCase.Output.Success -> {
                Timber.d("Sign in success")
                decryptChallenge(
                    currentMfaToken,
                    serverPublicKey,
                    passphrase,
                    userId,
                    result,
                    rsaKey,
                    onSuccess,
                    onError
                )
            }
        }
    }

    @Suppress("LongParameterList") // TODO extract member data class?
    private suspend fun decryptChallenge(
        currentMfaToken: String?,
        serverPublicKey: String,
        passphrase: ByteArray,
        userId: String,
        result: SignInUseCase.Output.Success,
        rsaKey: String,
        onSuccess: suspend (Success) -> Unit,
        onError: (Error) -> Unit
    ) {
        Timber.d("Decrypting challenge.")
        val challengeDecryptResult = challengeDecryptor.decrypt(
            serverPublicKey,
            passphrase,
            userId,
            result.challenge
        )
        when (challengeDecryptResult) {
            is ChallengeDecryptor.Output.DecryptedChallenge -> {
                Timber.d("Challenge decrypted successfully")
                verifyChallenge(
                    challengeDecryptResult.challenge,
                    result.mfaToken,
                    currentMfaToken,
                    rsaKey,
                    onError,
                    onSuccess
                )
            }
            is ChallengeDecryptor.Output.DecryptionError -> {
                challengeDecryptResult.message.let {
                    Timber.e("Challenge decryption error: $it")
                    onError(Error.ChallengeDecryptionError(it))
                }
            }
        }
    }

    private suspend fun verifyChallenge(
        challengeResponseDto: ChallengeResponseDto,
        mfaToken: String?,
        currentMfaToken: String?,
        rsaKey: String,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit
    ) {
        Timber.d("Verifying challenge")
        when (val result = challengeVerifier.verify(challengeResponseDto, rsaKey)) {
            ChallengeVerifier.Output.Failure -> {
                Timber.e("Challenge verification error")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.FAILURE))
            }
            ChallengeVerifier.Output.InvalidSignature -> {
                Timber.e("Challenge verification error: invalid signature")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.INVALID_SIGNATURE))
            }
            ChallengeVerifier.Output.TokenExpired -> {
                Timber.e("Challenge verification error: token expired")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.TOKEN_EXPIRED))
            }
            is ChallengeVerifier.Output.Verified -> {
                Timber.d("Challenge verified with success")
                Timber.d("Increasing sign in count")
                inAppReviewInteractor.processSuccessfulSignIn()
                onSuccess(
                    Success(
                        challengeResponseDto,
                        result.accessToken,
                        result.refreshToken,
                        mfaToken,
                        currentMfaToken
                    )
                )
            }
        }
    }

    data class Success(
        val challengeResponseDto: ChallengeResponseDto,
        val accessToken: String,
        val refreshToken: String,
        val mfaToken: String?,
        val currentMfaToken: String?
    )

    sealed class Error {
        object IncorrectPassphrase : Error()
        data class AccountDoesNotExist(val label: String, val email: String?, val serverUrl: String) : Error()
        data class SignInFailure(val message: String) : Error()
        data class ChallengeDecryptionError(val message: String?) : Error()
        data class ChallengeVerificationError(val type: Type) : Error() {
            enum class Type {
                INVALID_SIGNATURE,
                TOKEN_EXPIRED,
                FAILURE
            }
        }
    }
}
