package com.passbolt.mobile.android.metadata.interactor

import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.gopenpgp.model.VerifiedMessage
import com.passbolt.mobile.android.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.GetTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.GetTrustedMetadataKeyUseCase.Output.NoTrustedKey
import com.passbolt.mobile.android.metadata.usecase.GetTrustedMetadataKeyUseCase.Output.TrustedKey
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
import com.passbolt.mobile.android.ui.UserModel
import timber.log.Timber
import java.util.UUID

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

class MetadataPrivateKeysInteractor(
    private val getLocalMetadataKeysUseCase: GetLocalMetadataKeysUseCase,
    private val openPgp: OpenPgp,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getTrustedMetadataKeyUseCase: GetTrustedMetadataKeyUseCase,
    private val deleteTrustedMetadataKeyUseCase: DeleteTrustedMetadataKeyUseCase,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor
) {

    suspend fun verifyMetadataPrivateKey(): Output {
        Timber.d("Verifying metadata private key trust")

        val backendMetadataKey = getLocalMetadataKeysUseCase.execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT))
            .firstOrNull()
        val localTrustedKey = getTrustedMetadataKeyUseCase.execute(Unit)

        return if (backendMetadataKey != null) {
            verifyWithBackendMetadataKeyPresent(backendMetadataKey, localTrustedKey)
        } else {
            verifyWithNoBackendMetadataKey(localTrustedKey)
        }
    }

    private fun verifyWithNoBackendMetadataKey(localTrustedKey: GetTrustedMetadataKeyUseCase.Output): Output {
        Timber.d("Metadata key is not present server-side")

        return when (localTrustedKey) {
            is TrustedKey -> {
                Timber.d("Metadata key is present locally - trusted key to be deleted after confirmation")
                Output.TrustedKeyDeleted(
                    keyFingerprint = localTrustedKey.signingKeyFingerprint,
                    signedUsername = localTrustedKey.signedUsername,
                    signedName = localTrustedKey.signedName
                )
            }
            is NoTrustedKey -> {
                Timber.d("Metadata key is not present locally")
                Output.NoMetadataKey
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun verifyWithBackendMetadataKeyPresent(
        backendMetadataKey: ParsedMetadataKeyModel,
        localTrustedKey: GetTrustedMetadataKeyUseCase.Output
    ): Output {
        Timber.d("Metadata key is present server-side")

        val userWhoModifiedTheKey = runCatching {
            getLocalUserUseCase.execute(
                GetLocalUserUseCase.Input(backendMetadataKey.metadataPrivateKeys.first().modifiedBy.toString())
            ).user
        }.getOrNull()
        val currentUserPrivateKey = requireNotNull(getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey)
        val currentUserSigningKeyFingerprint = requireNotNull(
            (openPgp.getKeyFingerprint(currentUserPrivateKey) as? OpenPgpResult.Result)
        ).result
        val currentUserSigningKey = requireNotNull(
            (openPgp.generatePublicKey(currentUserPrivateKey) as? OpenPgpResult.Result)
        ).result

        if (userWhoModifiedTheKey == null) {
            Timber.e("Could not get the user who modified the key")
            return Output.CannotValidateSignature.CannotGetUser
        }

        val potentialPassphrase = passphraseMemoryCache.get()
        if (potentialPassphrase is PotentialPassphrase.PassphraseNotPresent) {
            Timber.d("Passphrase is not present in cache")
            return Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
        val passphrase = (potentialPassphrase as PotentialPassphrase.Passphrase).passphrase

        Timber.d("Verifying key signature")
        val verifiedMessage = openPgp.verifySignature(
            armoredPrivateKey = currentUserPrivateKey,
            passphrase = passphrase,
            armoredPublicKey = userWhoModifiedTheKey.gpgKey.armoredKey,
            pgpMessage = backendMetadataKey.metadataPrivateKeys.first().pgpMessage.toByteArray()
        )

        when (verifiedMessage) {
            is OpenPgpResult.Error -> {
                Timber.e("Signature is invalid: ${verifiedMessage.error.message}")
                return Output.CannotValidateSignature.SignatureInvalid(
                    keyFingerprint = userWhoModifiedTheKey.gpgKey.fingerprint,
                    signedUsername = userWhoModifiedTheKey.userName,
                    signedName = userWhoModifiedTheKey.fullName
                )
            }
            is OpenPgpResult.Result<VerifiedMessage> -> {
                return verifyWithBackendKeyPresentWithValidSignature(
                    localTrustedKey = localTrustedKey,
                    currentUserPrivateKey = currentUserPrivateKey,
                    currentUserPrivateKeyPassphrase = passphrase,
                    currentUserSigningKeyFingerprint = currentUserSigningKeyFingerprint,
                    currentUserSigningKey = currentUserSigningKey,
                    verifiedMessage = verifiedMessage.result,
                    backendMetadataKey = backendMetadataKey,
                    userWhoModifiedTheKey = userWhoModifiedTheKey
                )
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun verifyWithBackendKeyPresentWithValidSignature(
        localTrustedKey: GetTrustedMetadataKeyUseCase.Output,
        currentUserPrivateKey: String,
        currentUserPrivateKeyPassphrase: ByteArray,
        currentUserSigningKey: String,
        currentUserSigningKeyFingerprint: String,
        verifiedMessage: VerifiedMessage,
        backendMetadataKey: ParsedMetadataKeyModel,
        userWhoModifiedTheKey: UserModel
    ): Output {
        Timber.d("Metadata key signature is valid")

        return when (localTrustedKey) {
            is NoTrustedKey -> verifyWithBackendKeyPresentWithValidSignatureAndNoLocalKey(
                currentUserPrivateKey = currentUserPrivateKey,
                currentUserPrivateKeyPassphrase = currentUserPrivateKeyPassphrase,
                currentUserSigningKey = currentUserSigningKey,
                currentUserSigningKeyFingerprint = currentUserSigningKeyFingerprint,
                verifiedKeyPgpMessage = verifiedMessage,
                backendMetadataKey = backendMetadataKey,
                userWhoModifiedTheKey = userWhoModifiedTheKey
            )
            is TrustedKey -> verifyWithBackendKeyPresentWithValidSignatureAndWithLocalKey(
                verifiedKeyPgpMessage = verifiedMessage,
                backendMetadataKey = backendMetadataKey,
                userWhoModifiedTheKey = userWhoModifiedTheKey,
                localTrustedKey = localTrustedKey
            )
        }
    }

    @Suppress("LongMethod")
    private suspend fun verifyWithBackendKeyPresentWithValidSignatureAndWithLocalKey(
        verifiedKeyPgpMessage: VerifiedMessage,
        backendMetadataKey: ParsedMetadataKeyModel,
        userWhoModifiedTheKey: UserModel,
        localTrustedKey: TrustedKey
    ): Output {
        Timber.d("There is a local trusted key")

        val backendKeySignatureTimeStamp = verifiedKeyPgpMessage.signatureCreationTimestampSeconds
        val backendKeySigningKeyFingerprint = verifiedKeyPgpMessage.signatureKeyFingerprint

        val localKeySignatureTimeStamp = localTrustedKey.signatureCreationTimestampSeconds
        val localKeySigningKeyFingerprint = localTrustedKey.signingKeyFingerprint

        return if (backendKeySignatureTimeStamp == localKeySignatureTimeStamp &&
            backendKeySigningKeyFingerprint.equals(localKeySigningKeyFingerprint, ignoreCase = true)
        ) {
            Timber.d("Backend key and local key are the same - key is trusted")
            Output.KeyIsTrusted
        } else {
            Timber.d("Backend key and local key are not same")

            val backendNewKeyToTrustOutput = Output.NewKeyToTrust(
                id = backendMetadataKey.metadataPrivateKeys.first().id,
                signedUsername = userWhoModifiedTheKey.userName,
                signedName = userWhoModifiedTheKey.fullName,
                signatureCreationTimestampSeconds = verifiedKeyPgpMessage.signatureCreationTimestampSeconds,
                signatureKeyFingerprint = verifiedKeyPgpMessage.signatureKeyFingerprint,
                metadataPrivateKey = backendMetadataKey.metadataPrivateKeys.first()
            )

            if (!backendKeySigningKeyFingerprint.equals(localKeySigningKeyFingerprint, ignoreCase = true)) {
                Timber.d(
                    "Backend key and local key are signed with different keys " +
                            "- backend key trust needs confirmation"
                )
                backendNewKeyToTrustOutput
            } else {
                Timber.d("Key is signed by current user")

                if (backendKeySignatureTimeStamp < localKeySignatureTimeStamp) {
                    Timber.d(
                        "Backend key signature is older than local key signature" +
                                " - backend key needs confirmation"
                    )
                    backendNewKeyToTrustOutput
                } else {
                    Timber.d(
                        "Backend key signature is younger than local key signature " +
                                "- trusting backend key locally"
                    )

                    metadataPrivateKeysHelperInteractor.saveTrustedKeyToLocalStorage(backendNewKeyToTrustOutput)
                    Timber.d("Saved server metadata key to local storage - key is trusted")
                    Output.KeyIsTrusted
                }
            }
        }
    }

    private suspend fun verifyWithBackendKeyPresentWithValidSignatureAndNoLocalKey(
        currentUserPrivateKey: String,
        currentUserPrivateKeyPassphrase: ByteArray,
        currentUserSigningKey: String,
        currentUserSigningKeyFingerprint: String,
        verifiedKeyPgpMessage: VerifiedMessage,
        backendMetadataKey: ParsedMetadataKeyModel,
        userWhoModifiedTheKey: UserModel
    ): Output {
        Timber.d("There is no local trusted key")
        val signatureSigningKeyFingerprint = verifiedKeyPgpMessage.signatureKeyFingerprint

        return if (currentUserSigningKeyFingerprint.equals(signatureSigningKeyFingerprint, ignoreCase = true)) {
            Timber.d("Server metadata key is already signed by current user")
            metadataPrivateKeysHelperInteractor.saveTrustedKeyToLocalStorage(
                Output.NewKeyToTrust(
                    id = backendMetadataKey.metadataPrivateKeys.first().id,
                    signedUsername = userWhoModifiedTheKey.userName,
                    signedName = userWhoModifiedTheKey.fullName,
                    signatureCreationTimestampSeconds = verifiedKeyPgpMessage.signatureCreationTimestampSeconds,
                    signatureKeyFingerprint = verifiedKeyPgpMessage.signatureKeyFingerprint,
                    metadataPrivateKey = backendMetadataKey.metadataPrivateKeys.first()
                )
            )
            Timber.d("Saved server metadata key to local storage - key is trusted")
            Output.KeyIsTrusted
        } else {
            Timber.d("Server metadata key is not signed by current user - TOFU")
            val result = metadataPrivateKeysHelperInteractor.signTheKeyAndAddToLocalStorageAndPushToBackend(
                decryptedKey = verifiedKeyPgpMessage.decryptedMessage,
                metadataKey = backendMetadataKey,
                privateKey = currentUserPrivateKey,
                passphrase = currentUserPrivateKeyPassphrase,
                publicKey = currentUserSigningKey
            )

            when (result) {
                is MetadataPrivateKeysHelperInteractor.Output.CryptoFailure -> {
                    Timber.e("There was a crypto failure during sign and push the key: ${result.error.message}")
                    Output.Failure(result.authenticationState)
                }
                is MetadataPrivateKeysHelperInteractor.Output.KeyUploadFailure<*> -> {
                    Timber.e("There was a failure during upload the key: ${result.response.exception.message}")
                    Output.Failure(result.authenticationState)
                }
                is MetadataPrivateKeysHelperInteractor.Output.Success -> {
                    Timber.d("Signed the key and pushed to backend - key is trusted")
                    Output.KeyIsTrusted
                }
            }
        }
    }

    // TODO use in UI layer
    @Suppress("UnusedPrivateMember")
    private suspend fun deletedTrustedMetadataPrivateKey() {
        deleteTrustedMetadataKeyUseCase.execute(Unit)
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = if (this is Failure) {
                this.failureAuthenticationState
            } else {
                AuthenticationState.Authenticated
            }

        data object KeyIsTrusted : Output()

        data class TrustedKeyDeleted(
            val keyFingerprint: String,
            val signedUsername: String,
            val signedName: String
        ) : Output()

        data class NewKeyToTrust(
            val id: UUID,
            val signedUsername: String,
            val signedName: String,
            val signatureCreationTimestampSeconds: Long,
            val signatureKeyFingerprint: String,
            val metadataPrivateKey: ParsedMetadataPrivateKeyModel
        ) : Output()

        sealed class CannotValidateSignature : Output() {

            data object CannotGetUser : CannotValidateSignature()

            data class SignatureInvalid(
                val keyFingerprint: String,
                val signedUsername: String,
                val signedName: String
            ) : CannotValidateSignature()
        }

        data class Failure(val failureAuthenticationState: AuthenticationState) : Output()

        data object NoMetadataKey : Output()
    }
}
