package com.passbolt.mobile.android.domain.metadata.interactor

import com.google.gson.Gson
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.CompleteAuthenticatedOutput
import com.passbolt.mobile.android.core.mvp.authentication.IncompleteAuthenticatedOutput
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.usePassphraseCopy
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.SaveTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.domain.users.usecase.GetLocalUserUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.DecryptedMetadataPrivateKeyJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyModification
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
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

class MetadataPrivateKeysHelperInteractor(
    private val updateMetadataPrivateKeyUseCase: UpdateMetadataPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val saveTrustedMetadataKeyUseCase: SaveTrustedMetadataKeyUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val deleteTrustedMetadataKeyUseCase: DeleteTrustedMetadataKeyUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val metadataKeysInteractor: MetadataKeysInteractor,
    private val gson: Gson,
) {
    suspend fun saveTrustedKeyToLocalStorage(key: MetadataPrivateKeysInteractor.Output.NewKeyToTrust) {
        saveTrustedMetadataKeyUseCase.execute(
            SaveTrustedMetadataKeyUseCase.Input(
                id = key.id,
                userId = key.metadataPrivateKey.userId,
                keyData = key.metadataPrivateKey.keyData,
                passphrase = key.metadataPrivateKey.passphrase,
                created = key.metadataPrivateKey.created,
                createdBy = key.metadataPrivateKey.createdBy,
                modified = key.metadataPrivateKey.modified,
                modifiedBy = key.metadataPrivateKey.modifiedBy,
                keyPgpMessage = key.metadataPrivateKey.pgpMessage,
                signingKeyFingerprint = key.signatureKeyFingerprint,
                signatureCreationTimestampSeconds = key.signatureCreationTimestampSeconds,
                signedUsername = key.signedUsername,
                signedName = key.signedName,
            ),
        )
    }

    suspend fun trustNewKey(model: NewMetadataKeyToTrustModel): Output =
        try {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val currentUserPrivateKey = requireNotNull(privateKeyRepository.getPrivateKey(userId)?.armoredKey)
            val currentUserSigningKey =
                requireNotNull(
                    (openPgp.generatePublicKey(currentUserPrivateKey) as? OpenPgpResult.Result),
                ).result
            passphraseMemoryCache.usePassphraseCopy(
                onPassphraseNotPresent = { error("Passphrase not present in cache") },
            ) { passphrase ->
                signTheKeyAndAddToLocalStorageAndPushToBackend(
                    metadataPrivateKey = model.metadataPrivateKey,
                    privateKey = currentUserPrivateKey,
                    passphrase = passphrase,
                    publicKey = currentUserSigningKey,
                )
            }
        } catch (e: Exception) {
            val errorMessage = "Error while preparing the signed metadata key"
            Timber.e(e, errorMessage)
            Output.CryptoFailure(OpenPgpError(errorMessage))
        }

    suspend fun deletedTrustedMetadataPrivateKey() {
        deleteTrustedMetadataKeyUseCase.execute(Unit)
        Timber.d("Deleted trusted metadata private key")
    }

    suspend fun signTheKeyAndAddToLocalStorageAndPushToBackend(
        metadataPrivateKey: ParsedMetadataPrivateKeyModel,
        privateKey: String,
        passphrase: ByteArray,
        publicKey: String,
    ): Output {
        Timber.d("Signing the server key with current user's key")

        val pgpMessageSigned =
            openPgp.encryptSignMessageArmored(
                privateKey = privateKey,
                passphrase = passphrase,
                message =
                    gson.toJson(
                        DecryptedMetadataPrivateKeyJsonModel(
                            objectType = "PASSBOLT_METADATA_PRIVATE_KEY",
                            armoredKey = metadataPrivateKey.keyData,
                            passphrase = metadataPrivateKey.passphrase,
                            fingerprint = metadataPrivateKey.fingerprint,
                            domain = metadataPrivateKey.domain,
                        ),
                    ),
            )

        return when (pgpMessageSigned) {
            is OpenPgpResult.Error -> Output.CryptoFailure(pgpMessageSigned.error.pgpError)
            is OpenPgpResult.Result ->
                verifySignedSignatureAndSaveToLocalStorage(
                    pgpMessageSigned.result,
                    metadataPrivateKey,
                    privateKey,
                    passphrase,
                    publicKey,
                )
        }
    }

    private suspend fun verifySignedSignatureAndSaveToLocalStorage(
        pgpMessageSigned: String,
        metadataPrivateKey: ParsedMetadataPrivateKeyModel,
        privateKey: String,
        passphrase: ByteArray,
        publicKey: String,
    ): Output {
        val verifiedMessage =
            openPgp.verifySignature(
                armoredPrivateKey = privateKey,
                passphrase = passphrase,
                armoredPublicKey = publicKey,
                pgpMessage = pgpMessageSigned.toByteArray(),
            )

        return when (verifiedMessage) {
            is OpenPgpResult.Error -> Output.CryptoFailure(verifiedMessage.error.pgpError)
            is OpenPgpResult.Result -> {
                val currentUserServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
                val currentUser = getLocalUserUseCase.execute(GetLocalUserUseCase.Input(currentUserServerId)).user

                Timber.d("Saving signed key to local storage")
                saveTrustedKeyToLocalStorage(
                    MetadataPrivateKeysInteractor.Output.NewKeyToTrust(
                        id = metadataPrivateKey.id,
                        signedUsername = currentUser.userName,
                        signedName = currentUser.fullName,
                        signatureCreationTimestampSeconds = verifiedMessage.result.signatureCreationTimestampSeconds,
                        signatureKeyFingerprint = verifiedMessage.result.signatureKeyFingerprint,
                        metadataPrivateKey = metadataPrivateKey.copy(pgpMessage = pgpMessageSigned),
                        modificationKind = MetadataKeyModification.FORWARD_TRUST,
                    ),
                )
                Timber.d("Saved signed key to local storage")

                pushKeyToBackend(metadataPrivateKey, pgpMessageSigned)
            }
        }
    }

    private suspend fun pushKeyToBackend(
        metadataPrivateKey: ParsedMetadataPrivateKeyModel,
        pgpMessageSigned: String,
    ): Output {
        Timber.d("Pushing the signed key to the backend")

        val result =
            updateMetadataPrivateKeyUseCase.execute(
                UpdateMetadataPrivateKeyUseCase.Input(
                    metadataPrivateKeyId = metadataPrivateKey.id.toString(),
                    privateKeyPgpMessage = pgpMessageSigned,
                ),
            )

        return when (result) {
            is UpdateMetadataPrivateKeyUseCase.Output.Failure -> Output.KeyUploadFailure(result.incomplete)
            is UpdateMetadataPrivateKeyUseCase.Output.Success -> {
                when (metadataKeysInteractor.fetchAndSaveMetadataKeys()) {
                    is MetadataKeysInteractor.Output.Failure -> {
                        Timber.e("Failed to refresh local keys; Manual refresh required")
                        Output.Success
                    }
                    is MetadataKeysInteractor.Output.Success,
                    is MetadataKeysInteractor.Output.InvalidEncryptionKey,
                    -> Output.Success
                }
            }
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success :
            Output(),
            CompleteAuthenticatedOutput

        data class CryptoFailure(
            val error: OpenPgpError,
        ) : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Unauthenticated(Passphrase)
        }

        data class KeyUploadFailure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput
    }
}
