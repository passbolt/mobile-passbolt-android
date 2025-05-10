package com.passbolt.mobile.android.metadata.interactor

import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.metadata.usecase.SaveTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
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
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase
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
                signedName = key.signedName
            )
        )
    }

    suspend fun signTheKeyAndAddToLocalStorageAndPushToBackend(
        decryptedKey: String,
        metadataKey: ParsedMetadataKeyModel,
        privateKey: String,
        passphrase: ByteArray,
        publicKey: String
    ): Output {
        Timber.d("Signing the server key with current user's key")

        val pgpMessageSigned = openPgp.encryptSignMessageArmored(
            privateKey = privateKey,
            passphrase = passphrase,
            message = decryptedKey
        )

        return when (pgpMessageSigned) {
            is OpenPgpResult.Error -> Output.CryptoFailure(pgpMessageSigned.error)
            is OpenPgpResult.Result -> verifySignedSignatureAndSaveToLocalStorage(
                pgpMessageSigned.result,
                metadataKey,
                privateKey,
                passphrase,
                publicKey
            )
        }
    }

    private suspend fun verifySignedSignatureAndSaveToLocalStorage(
        pgpMessageSigned: String,
        metadataKey: ParsedMetadataKeyModel,
        privateKey: String,
        passphrase: ByteArray,
        publicKey: String
    ): Output {
        val verifiedMessage = openPgp.verifySignature(
            armoredPrivateKey = privateKey,
            passphrase = passphrase,
            armoredPublicKey = publicKey,
            pgpMessage = pgpMessageSigned.toByteArray()
        )

        return when (verifiedMessage) {
            is OpenPgpResult.Error -> Output.CryptoFailure(verifiedMessage.error)
            is OpenPgpResult.Result -> {
                val currentUserServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
                val currentUser = getLocalUserUseCase.execute(GetLocalUserUseCase.Input(currentUserServerId)).user

                Timber.d("Saving signed key to local storage")
                saveTrustedKeyToLocalStorage(
                    MetadataPrivateKeysInteractor.Output.NewKeyToTrust(
                        id = metadataKey.metadataPrivateKeys.first().id,
                        signedUsername = currentUser.userName,
                        signedName = currentUser.fullName,
                        signatureCreationTimestampSeconds = verifiedMessage.result.signatureCreationTimestampSeconds,
                        signatureKeyFingerprint = verifiedMessage.result.signatureKeyFingerprint,
                        metadataPrivateKey = metadataKey.metadataPrivateKeys.first()
                            .copy(pgpMessage = pgpMessageSigned)
                    )
                )

                pushKeyToBackend(metadataKey, pgpMessageSigned)
            }
        }
    }

    private suspend fun pushKeyToBackend(metadataKey: ParsedMetadataKeyModel, pgpMessageSigned: String): Output {
        Timber.d("Pushing the signed key to the backend")

        val result = updateMetadataPrivateKeyUseCase.execute(
            UpdateMetadataPrivateKeyUseCase.Input(
                metadataPrivateKeyId = metadataKey.metadataPrivateKeys.first().id.toString(),
                privateKeyPgpMessage = pgpMessageSigned
            )
        )

        return when (result) {
            is UpdateMetadataPrivateKeyUseCase.Output.Failure<*> -> Output.KeyUploadFailure(result.response)
            is UpdateMetadataPrivateKeyUseCase.Output.Success -> Output.Success
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = when {
                this is KeyUploadFailure<*> && this.response.isUnauthorized ->
                    AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                this is KeyUploadFailure<*> && this.response.isMfaRequired -> {
                    val providers = MfaTypeProvider.get(this.response)

                    AuthenticationState.Unauthenticated(
                        AuthenticationState.Unauthenticated.Reason.Mfa(providers)
                    )
                }
                this is CryptoFailure ->
                    AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Passphrase)
                else -> AuthenticationState.Authenticated
            }

        data object Success : Output()

        data class CryptoFailure(val error: OpenPgpError) : Output()

        data class KeyUploadFailure<T : Any>(val response: NetworkResult.Failure<T>) : Output()
    }
}
