package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.mappers.AccountModelMapper
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
) {
    suspend fun signInVerify(
        serverPgpPublicKey: String,
        passphrase: ByteArray,
        userId: String,
        serverRsaKey: String,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Preparing sign in challenge")

        val configInput =
            SignInConfigInput(
                serverPgpPublicKey = serverPgpPublicKey,
                passphrase = passphrase,
                userId = userId,
                serverRsaKey = serverRsaKey,
            )

        val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
        val challenge =
            challengeProvider.get(
                domain = accountData.url,
                serverPublicKey = serverPgpPublicKey,
                passphrase = passphrase,
                userId = userId,
            )

        when (challenge) {
            is ChallengeProvider.Output.Success -> {
                Timber.d("Prepared sign in challenge")
                sendSignInRequest(
                    input =
                        SendSignInRequestInput(
                            config = configInput,
                            challenge = challenge.challenge,
                            accountData = accountData,
                            serverId = requireNotNull(accountData.serverId),
                        ),
                    onError = onError,
                    onSuccess = onSuccess,
                )
            }
            ChallengeProvider.Output.WrongPassphrase -> {
                Timber.d("Error during preparing challenge - incorrect passphrase")
                onError(Error.IncorrectPassphrase)
            }
        }
    }

    private suspend fun sendSignInRequest(
        input: SendSignInRequestInput,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Signing in")
        val currentMfaToken = getSessionUseCase.execute(Unit).mfaToken

        val signInInput =
            SignInUseCase.Input(
                userId = input.serverId,
                challenge = input.challenge,
                mfaToken = currentMfaToken,
            )

        when (val result = signInUseCase.execute(signInInput)) {
            is SignInUseCase.Output.Failure -> {
                Timber.e("Failure during sign in: ${result.message}")
                val error =
                    when (result.type) {
                        SignInFailureType.ACCOUNT_DOES_NOT_EXIST -> {
                            Error.AccountDoesNotExist(
                                label =
                                    input.accountData.label ?: AccountModelMapper.defaultLabel(
                                        input.accountData.firstName,
                                        input.accountData.lastName,
                                    ),
                                email = input.accountData.email,
                                serverUrl = input.accountData.url,
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
                    input =
                        DecryptChallengeInput(
                            config = input.config,
                            currentMfaToken = currentMfaToken,
                            signInResult = result,
                        ),
                    onSuccess = onSuccess,
                    onError = onError,
                )
            }
        }
    }

    private suspend fun decryptChallenge(
        input: DecryptChallengeInput,
        onSuccess: suspend (Success) -> Unit,
        onError: (Error) -> Unit,
    ) {
        Timber.d("Decrypting challenge.")
        val challengeDecryptResult =
            challengeDecryptor.decrypt(
                serverPublicKey = input.config.serverPgpPublicKey,
                passphrase = input.config.passphrase,
                userId = input.config.userId,
                challenge = input.signInResult.challenge,
            )

        when (challengeDecryptResult) {
            is ChallengeDecryptor.Output.DecryptedChallenge -> {
                Timber.d("Challenge decrypted successfully")
                verifyChallenge(
                    input =
                        VerifyChallengeInput(
                            challengeResponseDto = challengeDecryptResult.challenge,
                            mfaToken = input.signInResult.mfaToken,
                            currentMfaToken = input.currentMfaToken,
                            rsaKey = input.config.serverRsaKey,
                        ),
                    onError = onError,
                    onSuccess = onSuccess,
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
        input: VerifyChallengeInput,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Verifying challenge")
        when (val result = challengeVerifier.verify(input.challengeResponseDto, input.rsaKey)) {
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
                onSuccess(
                    Success(
                        challengeResponseDto = input.challengeResponseDto,
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        mfaToken = input.mfaToken,
                        currentMfaToken = input.currentMfaToken,
                    ),
                )
            }
        }
    }

    private class SignInConfigInput(
        val serverPgpPublicKey: String,
        val passphrase: ByteArray,
        val userId: String,
        val serverRsaKey: String,
    )

    private data class SendSignInRequestInput(
        val config: SignInConfigInput,
        val challenge: String,
        val accountData: GetAccountDataUseCase.Output,
        val serverId: String,
    )

    private data class DecryptChallengeInput(
        val config: SignInConfigInput,
        val currentMfaToken: String?,
        val signInResult: SignInUseCase.Output.Success,
    )

    private data class VerifyChallengeInput(
        val challengeResponseDto: ChallengeResponseDto,
        val mfaToken: String?,
        val currentMfaToken: String?,
        val rsaKey: String,
    )

    data class Success(
        val challengeResponseDto: ChallengeResponseDto,
        val accessToken: String,
        val refreshToken: String,
        val mfaToken: String?,
        val currentMfaToken: String?,
    )

    sealed class Error {
        data object IncorrectPassphrase : Error()

        data class AccountDoesNotExist(
            val label: String,
            val email: String?,
            val serverUrl: String,
        ) : Error()

        data class SignInFailure(
            val message: String,
        ) : Error()

        data class ChallengeDecryptionError(
            val message: String?,
        ) : Error()

        data class ChallengeVerificationError(
            val type: Type,
        ) : Error() {
            enum class Type {
                INVALID_SIGNATURE,
                TOKEN_EXPIRED,
                FAILURE,
            }
        }
    }
}
