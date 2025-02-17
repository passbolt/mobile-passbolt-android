package com.passbolt.mobile.android.metadata.interactor

import com.google.gson.Gson
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.dto.PassphraseNotInCacheException
import com.passbolt.mobile.android.dto.request.SessionKeysBundleDto
import com.passbolt.mobile.android.dto.response.DecryptedMetadataSessionKeysBundleModel
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.MetadataMapper
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysBundleMerger
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysBundleValidator
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysMemoryCache
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataSessionKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataSessionKeysUseCase.Output.Success
import com.passbolt.mobile.android.metadata.usecase.PostMetadataSessionKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.UpdateMetadataSessionKeysUseCase
import com.passbolt.mobile.android.ui.MetadataSessionKeysBundleModel
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

class MetadataSessionKeysInteractor(
    private val fetchMetadataSessionKeysUseCase: FetchMetadataSessionKeysUseCase,
    private val postMetadataSessionKeysUseCase: PostMetadataSessionKeysUseCase,
    private val updateMetadataSessionKeysUseCase: UpdateMetadataSessionKeysUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val sessionKeysBundleMerger: SessionKeysBundleMerger,
    private val sessionKeysMemoryCache: SessionKeysMemoryCache,
    private val metadataMapper: MetadataMapper,
    private val gson: Gson,
    private val sessionKeysBundleValidator: SessionKeysBundleValidator
) {

    suspend fun fetchMetadataSessionKeys(): Output {
        return when (val response = fetchMetadataSessionKeysUseCase.execute(Unit)) {
            is Success -> {
                try {
                    buildMetadataSessionKeysCache(response.metadataSessionKeysBundles)
                } catch (e: PassphraseNotInCacheException) {
                    Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
                }
            }
            is FetchMetadataSessionKeysUseCase.Output.Failure<*> ->
                Output.Failure(response.authenticationState)
        }
    }

    @Throws(PassphraseNotInCacheException::class)
    private suspend fun buildMetadataSessionKeysCache(
        metadataKeysBundles: List<MetadataSessionKeysBundleModel>
    ): Output {
        val privateKey = getPrivateKeyUseCase.execute(Unit).privateKey
        if (privateKey == null) {
            Timber.e("User private key not found")
            return Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
        return when (val passphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                Timber.d("Building session keys cache; Bundles count: ${metadataKeysBundles.size}")
                metadataKeysBundles
                    .mapDecryptNotNull(privateKey, passphrase.passphrase)
                    .let {
                        Timber.d("Merging session keys cache")
                        sessionKeysBundleMerger.merge(it)
                    }
                    .let {
                        Timber.d("Session keys cache loaded")
                        sessionKeysMemoryCache.value = it
                    }
                Output.Success
            }
            is PotentialPassphrase.PassphraseNotPresent ->
                throw PassphraseNotInCacheException()
        }
    }

    suspend fun saveMetadataSessionKeysCache(): Output {
        Timber.d("Saving session keys cache")
        val privateKey = getPrivateKeyUseCase.execute(Unit).privateKey
        if (privateKey == null) {
            Timber.e("User private key not found")
            return Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
        return when (val passphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                val mappedCache = metadataMapper.map(sessionKeysMemoryCache.value.keys)

                when (val encryptedCacheResult = openPgp.encryptSignMessageArmored(
                    privateKey,
                    passphrase.passphrase,
                    gson.toJson(mappedCache)
                )) {
                    is OpenPgpResult.Error -> {
                        Timber.e("Error when encrypting session keys cache")
                        // error when processing session key is not blocking
                        Output.Success
                    }
                    is OpenPgpResult.Result -> {
                        Timber.d("Encrypted session keys cache")

                        postOrUpdateCache(encryptedCacheResult)
                    }
                }
            }
            is PotentialPassphrase.PassphraseNotPresent ->
                Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
    }

    // if the origin cache was empty - post a new cache
    // if not update most recent one (by modified date)
    private suspend fun postOrUpdateCache(encryptedCacheResult: OpenPgpResult.Result<String>) =
        if (sessionKeysMemoryCache.wasInitialCacheEmpty) {
            Timber.d("No cached bundles initially existing - posting a new bundle")
            postNewSessionKeysCache(encryptedCacheResult.result)
        } else {
            if (sessionKeysMemoryCache.isLocallyModified) {
                Timber.d(
                    "Cached bundles existing and cache locally modified - " +
                            "updating the latest bundle"
                )
                updateExistingSessionKeysCache(
                    encryptedCacheResult.result,
                    restart = true
                )
            } else {
                Timber.d(
                    "Skipping session keys update - no local modifications"
                )
                Output.Success
            }
        }

    private suspend fun postNewSessionKeysCache(encryptedData: String): Output =
        when (postMetadataSessionKeysUseCase.execute(
            PostMetadataSessionKeysUseCase.Input(encryptedData)
        )
        ) {
            is PostMetadataSessionKeysUseCase.Output.Failure<*> -> {
                Timber.e("Error when posting session keys cache")
                // error when processing session key is not blocking
                Output.Success
            }
            is PostMetadataSessionKeysUseCase.Output.Success -> {
                Timber.d("New session keys cache saved")
                Output.Success
            }
        }

    private suspend fun updateExistingSessionKeysCache(
        encryptedData: String,
        restart: Boolean
    ): Output {
        val latestModifiedOrigin = sessionKeysMemoryCache.findLatestModifiedOriginBundleData()
        require(latestModifiedOrigin != null) { "No origin bundle found but trying to update" }
        return when (updateMetadataSessionKeysUseCase.execute(
            UpdateMetadataSessionKeysUseCase.Input(
                metadataBundleId = latestModifiedOrigin.originBundleId,
                modifiedDate = latestModifiedOrigin.modifiedDate,
                encryptedData = encryptedData
            )
        )
        ) {
            is UpdateMetadataSessionKeysUseCase.Output.Failure<*> -> {
                Timber.e("Error when updating session keys cache")
                // error when processing session key is not blocking
                Output.Success
            }
            // there might be a conflict when other client updates the session cache in the meantime
            UpdateMetadataSessionKeysUseCase.Output.Conflict -> {
                Timber.d(
                    "Conflict when updating session keys cache, " +
                            "trying to re-fetch and restart update; restart=$restart"
                )
                if (restart) {
                    tryReFetchCacheAndUpdate()
                }
                // error when processing session key is not blocking
                Output.Success
            }
            is UpdateMetadataSessionKeysUseCase.Output.Success -> {
                Timber.d("Existing session keys cache updated")
                Output.Success
            }
        }
    }

    private suspend fun tryReFetchCacheAndUpdate() {
        val reFetchedCache =
            (fetchMetadataSessionKeysUseCase.execute(Unit) as? Success)?.metadataSessionKeysBundles
        val privateKey = getPrivateKeyUseCase.execute(Unit).privateKey
        val passphrase = passphraseMemoryCache.get()
        if (reFetchedCache != null && privateKey != null && passphrase is PotentialPassphrase.Passphrase) {
            val localSessionKeysBundleId = UUID.randomUUID()
            val mergedLocalWithReFetched = sessionKeysBundleMerger.merge(
                reFetchedCache.mapDecryptNotNull(privateKey, passphrase.passphrase) +
                        metadataMapper.map(sessionKeysMemoryCache.value, localSessionKeysBundleId)
            )
            // local cache bundle is not part of origin
            mergedLocalWithReFetched.originMetadata.remove(localSessionKeysBundleId.toString())
            sessionKeysMemoryCache.value = mergedLocalWithReFetched
            val encryptedCache = openPgp.encryptSignMessageArmored(
                privateKey,
                passphrase.passphrase,
                gson.toJson(metadataMapper.map(sessionKeysMemoryCache.value.keys))
            )
            if (encryptedCache is OpenPgpResult.Result) {
                updateExistingSessionKeysCache(encryptedCache.result, restart = false)
            }
        }
    }

    private suspend fun List<MetadataSessionKeysBundleModel>.mapDecryptNotNull(
        privateKey: String,
        passphrase: ByteArray
    ) =
        mapNotNull { metadataSessionKeysBundle ->
            when (val decryptedBundleResult = openPgp.decryptVerifyMessageArmored(
                privateKey,
                passphrase,
                metadataSessionKeysBundle.data
            )) {
                is OpenPgpResult.Error -> {
                    Timber.e("Error when decrypting session keys bundle")
                    null
                }
                is OpenPgpResult.Result -> {
                    Timber.d("Decrypted session keys bundle")
                    val bundleModel = DecryptedMetadataSessionKeysBundleModel(
                        id = metadataSessionKeysBundle.id,
                        bundle = gson.fromJson(
                            String(decryptedBundleResult.result),
                            SessionKeysBundleDto::class.java
                        ),
                        created = metadataSessionKeysBundle.created,
                        modified = metadataSessionKeysBundle.modified
                    )
                    if (sessionKeysBundleValidator.isValid(bundleModel.bundle)) {
                        bundleModel
                    } else {
                        Timber.e("Invalid session keys bundle: ${metadataSessionKeysBundle.id}")
                        null
                    }
                }
            }
        }

    sealed class Output : AuthenticatedUseCaseOutput {

        data object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        data class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
