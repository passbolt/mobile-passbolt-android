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
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.MetadataMapper
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysBundleMerger
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysMemoryCache
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataSessionKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.SaveMetadataSessionKeysUseCase
import com.passbolt.mobile.android.ui.MetadataSessionKeysBundleModel
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

class MetadataSessionKeysInteractor(
    private val fetchMetadataSessionKeysUseCase: FetchMetadataSessionKeysUseCase,
    private val saveMetadataSessionKeysUseCase: SaveMetadataSessionKeysUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val sessionKeysBundleMerger: SessionKeysBundleMerger,
    private val sessionKeysMemoryCache: SessionKeysMemoryCache,
    private val metadataMapper: MetadataMapper,
    private val gson: Gson
) {

    suspend fun fetchMetadataSessionKeys(): Output {
        return when (val response = fetchMetadataSessionKeysUseCase.execute(Unit)) {
            is FetchMetadataSessionKeysUseCase.Output.Success -> {
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
                metadataKeysBundles.mapNotNull { metadataSessionKeysBundle ->
                    when (val decryptedBundleResult = openPgp.decryptVerifyMessageArmored(
                        privateKey,
                        passphrase.passphrase,
                        metadataSessionKeysBundle.data
                    )) {
                        is OpenPgpResult.Error -> {
                            Timber.e("Error when decrypting session keys bundle")
                            null
                        }
                        is OpenPgpResult.Result -> {
                            gson.fromJson(
                                String(decryptedBundleResult.result),
                                SessionKeysBundleDto::class.java
                            )
                        }
                    }
                }
                    .let { sessionKeysBundleMerger.merge(it) }
                    .let { sessionKeysMemoryCache.value = it }
                Output.Success
            }
            is PotentialPassphrase.PassphraseNotPresent ->
                throw PassphraseNotInCacheException()
        }
    }

    suspend fun saveMetadataSessionKeysCache(): Output {
        val privateKey = getPrivateKeyUseCase.execute(Unit).privateKey
        if (privateKey == null) {
            Timber.e("User private key not found")
            return Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
        return when (val passphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                val mappedCache = metadataMapper.map(sessionKeysMemoryCache.value)

                when (val encryptedCacheResult = openPgp.encryptSignMessageArmored(
                    privateKey,
                    passphrase.passphrase,
                    gson.toJson(mappedCache)
                )) {
                    is OpenPgpResult.Error -> {
                        Timber.e("Error when encrypting session keys cache")
                        // error when posting cache is not a blocking error
                        Output.Success
                    }
                    is OpenPgpResult.Result -> {
                        when (val saveCacheResult = saveMetadataSessionKeysUseCase.execute(
                            SaveMetadataSessionKeysUseCase.Input(encryptedCacheResult.result)
                        )
                        ) {
                            is SaveMetadataSessionKeysUseCase.Output.Failure<*> -> {
                                Timber.e("Error when saving session keys cache")
                                Output.Failure(saveCacheResult.authenticationState)
                            }
                            is SaveMetadataSessionKeysUseCase.Output.Success -> Output.Success
                        }
                    }
                }
                Output.Success
            }
            is PotentialPassphrase.PassphraseNotPresent ->
                Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
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
