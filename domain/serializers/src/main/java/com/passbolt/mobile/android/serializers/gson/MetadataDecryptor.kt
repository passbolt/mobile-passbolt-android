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

package com.passbolt.mobile.android.serializers.gson

import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto.PERSONAL
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto.SHARED
import com.passbolt.mobile.android.dto.response.ResourceResponseV5Dto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.metadata.sessionkeys.ForeignModel.RESOURCE
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysMemoryCache
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import timber.log.Timber

class MetadataDecryptor(
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val metadataKeys: List<ParsedMetadataKeyModel>,
    private val openPgp: OpenPgp,
    private val sessionKeysCache: SessionKeysMemoryCache,
) {
    suspend fun decryptMetadata(resource: ResourceResponseV5Dto): Output {
        return try {
            val (key, passphrase) = getKeyAndPassphrase(resource)

            // decrypt using cached session key
            if (sessionKeysCache.hasCachedKey(RESOURCE.value, resource.id)) {
                val cachedSessionKey =
                    // not null because of hasCachedKey check
                    requireNotNull(
                        sessionKeysCache.getSessionKeyHexString(RESOURCE.value, resource.id),
                    )
                val decryptUsingCachedResult =
                    openPgp.decryptMessageArmoredWithSessionKey(
                        cachedSessionKey,
                        resource.metadata,
                    )
                // if result is success, return it; otherwise, fallback to full decrypt
                // session key may not be valid but we know it only when trying to use it and it fails
                if (decryptUsingCachedResult is OpenPgpResult.Result) {
                    return Output.Success(decryptUsingCachedResult.result)
                }
            }

            // fallback to full decrypt
            val sessionKey = openPgp.decryptSessionKey(key, passphrase, resource.metadata)
            require(sessionKey is OpenPgpResult.Result) {
                "Failed to decrypt session key id=(${resource.id}), skipping"
            }

            sessionKeysCache.put(RESOURCE.value, resource.id, sessionKey.result)

            val decryptedMetadata =
                openPgp.decryptMessageArmoredWithSessionKey(
                    sessionKey.result,
                    resource.metadata,
                )

            require(decryptedMetadata is OpenPgpResult.Result) {
                "Failed to decrypt resource id=(${resource.id}), skipping"
            }

            Output.Success(decryptedMetadata.result)
        } catch (exception: Exception) {
            Timber.e(exception, "Exception during metadata decryption")
            Output.Failure(exception)
        }
    }

    private fun getKeyAndPassphrase(resource: ResourceResponseV5Dto): KeyToPassphrase =
        when (resource.metadataKeyType) {
            SHARED -> {
                val metadataPrivateKey =
                    metadataKeys
                        .firstOrNull { it.id == resource.metadataKeyId }
                        ?.metadataPrivateKeys
                        ?.firstOrNull()

                require(metadataPrivateKey != null) {
                    "Metadata private key for resource id=(${resource.id}) not found, skipping"
                }

                metadataPrivateKey.keyData to metadataPrivateKey.passphrase.toByteArray()
            }
            PERSONAL -> {
                val privateKey = getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey
                require(privateKey != null) { "Selected user private key not found" }
                val passphrase = passphraseMemoryCache.get()
                require(passphrase is PotentialPassphrase.Passphrase) { "Passphrase not present in cache" }
                privateKey to passphrase.passphrase
            }
        }

    sealed class Output {
        data class Success(
            val decryptedMetadata: String,
        ) : Output()

        data class Failure(
            val error: Throwable?,
        ) : Output()
    }
}
