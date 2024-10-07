package com.passbolt.mobile.android.metadata.interactor

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.ui.MetadataKeyModel
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

class MetadataKeysInteractor(
    private val fetchMetadataKeysUseCase: FetchMetadataKeysUseCase,
    private val rebuildMetadataKeysTablesUseCase: RebuildMetadataKeysTablesUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val openPgp: OpenPgp
) {

    suspend fun fetchAndSaveMetadataKeys(): Output {
        return when (val response = fetchMetadataKeysUseCase.execute(Unit)) {
            is FetchMetadataKeysUseCase.Output.Success -> {
                saveMetadataKeys(response.metadataKeysModel)
            }
            is FetchMetadataKeysUseCase.Output.Failure<*> ->
                Output.Failure(response.authenticationState)
        }
    }

    private suspend fun saveMetadataKeys(metadataKeysModel: List<MetadataKeyModel>): Output {
        val privateKey = getPrivateKeyUseCase.execute(Unit).privateKey
        if (privateKey == null) {
            Timber.e("User private key not found")
            return Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
        return when (val passphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                val decryptedKeysModel = metadataKeysModel.map {
                    it.copy(metadataPrivateKeys = it.metadataPrivateKeys.mapNotNull { metadataPrivateKey ->
                        val decryptedKeyData = openPgp.decryptMessageArmored(
                            privateKey,
                            passphrase.passphrase,
                            metadataPrivateKey.keyData
                        )
                        when (decryptedKeyData) {
                            is OpenPgpResult.Error -> null
                            is OpenPgpResult.Result -> metadataPrivateKey.copy(
                                keyData = String(decryptedKeyData.result)
                            )
                        }
                    })
                }
                rebuildMetadataKeysTablesUseCase.execute(
                    RebuildMetadataKeysTablesUseCase.Input(decryptedKeysModel)
                )
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
