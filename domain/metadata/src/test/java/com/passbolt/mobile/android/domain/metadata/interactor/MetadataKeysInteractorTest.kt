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

package com.passbolt.mobile.android.domain.metadata.interactor

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator
import com.passbolt.mobile.android.domain.metadata.usecase.FetchMetadataKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.domain.privatekey.model.PrivateKey
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.DecryptedMetadataPrivateKeyJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyModel
import com.passbolt.mobile.android.ui.MetadataPrivateKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

class MetadataKeysInteractorTest {
    private val fetchMetadataKeysUseCase = mock<FetchMetadataKeysUseCase>()
    private val rebuildMetadataKeysTablesUseCase = mock<RebuildMetadataKeysTablesUseCase>()
    private val passphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
    private val privateKeyRepository = mock<PrivateKeyRepository>()
    private val openPgp = mock<OpenPgp>()
    private val gson = Gson()

    private val interactor =
        MetadataKeysInteractor(
            fetchMetadataKeysUseCase = fetchMetadataKeysUseCase,
            rebuildMetadataKeysTablesUseCase = rebuildMetadataKeysTablesUseCase,
            passphraseMemoryCache = passphraseMemoryCache,
            getSelectedAccountUseCase = getSelectedAccountUseCase,
            privateKeyRepository = privateKeyRepository,
            openPgp = openPgp,
            gson = gson,
            metadataKeysValidator = MetadataKeysValidator(openPgp),
            coroutineLaunchContext = TestCoroutineLaunchContext(),
        )

    @Before
    fun setUp() =
        runTest {
            whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(GetSelectedAccountUseCase.Output(USER_ID))
            whenever(privateKeyRepository.getPrivateKey(USER_ID)).thenReturn(PrivateKey(USER_PRIVATE_KEY))
            whenever(passphraseMemoryCache.get()).thenAnswer {
                PotentialPassphrase.Passphrase(PASSPHRASE.toByteArray())
            }
            whenever(fetchMetadataKeysUseCase.execute(Unit))
                .thenReturn(FetchMetadataKeysUseCase.Output.Success(listOf(metadataKey())))
            whenever(openPgp.decryptMessageArmored(any(), any(), any()))
                .thenReturn(OpenPgpResult.Result(decryptedPrivateKeyJson()))
            whenever(openPgp.getKeyFingerprint(PUBLIC_KEY)).thenReturn(OpenPgpResult.Result(FINGERPRINT))
            whenever(openPgp.getKeyFingerprint(PRIVATE_KEY)).thenReturn(OpenPgpResult.Result(FINGERPRINT))
        }

    @Test
    fun `metadata key with matching public and private fingerprints is persisted with its private key`() =
        runTest {
            val result = interactor.fetchAndSaveMetadataKeys()

            assertThat(result).isEqualTo(MetadataKeysInteractor.Output.Success)
            val persistedKeys = persistedKeys()
            assertThat(persistedKeys).hasSize(1)
            assertThat(persistedKeys.single().fingerprint).isEqualTo(FINGERPRINT)
            val persistedPrivateKey = persistedKeys.single().metadataPrivateKeys.single()
            assertThat(persistedPrivateKey.keyData).isEqualTo(PRIVATE_KEY)
            assertThat(persistedPrivateKey.fingerprint).isEqualTo(FINGERPRINT)
        }

    @Test
    fun `metadata key whose declared fingerprint does not match its key data is not persisted`() =
        runTest {
            whenever(fetchMetadataKeysUseCase.execute(Unit))
                .thenReturn(FetchMetadataKeysUseCase.Output.Success(listOf(metadataKey(fingerprint = OTHER_FINGERPRINT))))

            val result = interactor.fetchAndSaveMetadataKeys()

            assertThat(result).isEqualTo(MetadataKeysInteractor.Output.InvalidEncryptionKey)
            assertThat(persistedKeys()).isEmpty()
            verify(openPgp, never()).decryptMessageArmored(any(), any(), any())
        }

    @Test
    fun `encryption key left without any verified private key is reported as invalid`() =
        runTest {
            whenever(openPgp.decryptMessageArmored(any(), any(), any()))
                .thenReturn(OpenPgpResult.Result(decryptedPrivateKeyJson(fingerprint = OTHER_FINGERPRINT)))

            val result = interactor.fetchAndSaveMetadataKeys()

            assertThat(result).isEqualTo(MetadataKeysInteractor.Output.InvalidEncryptionKey)
        }

    @Test
    fun `rejected expired metadata key does not invalidate the encryption key`() =
        runTest {
            whenever(fetchMetadataKeysUseCase.execute(Unit))
                .thenReturn(
                    FetchMetadataKeysUseCase.Output.Success(
                        listOf(metadataKey(fingerprint = OTHER_FINGERPRINT, expired = ZonedDateTime.parse(TIMESTAMP))),
                    ),
                )

            val result = interactor.fetchAndSaveMetadataKeys()

            assertThat(result).isEqualTo(MetadataKeysInteractor.Output.Success)
            assertThat(persistedKeys()).isEmpty()
        }

    @Test
    fun `encryption key not shared with the user is persisted without being reported as invalid`() =
        runTest {
            whenever(fetchMetadataKeysUseCase.execute(Unit))
                .thenReturn(FetchMetadataKeysUseCase.Output.Success(listOf(metadataKey(metadataPrivateKeys = emptyList()))))

            val result = interactor.fetchAndSaveMetadataKeys()

            assertThat(result).isEqualTo(MetadataKeysInteractor.Output.Success)
            assertThat(persistedKeys().single().metadataPrivateKeys).isEmpty()
        }

    @Test
    fun `private key whose declared fingerprint does not match its key data is dropped`() =
        runTest {
            whenever(openPgp.decryptMessageArmored(any(), any(), any()))
                .thenReturn(OpenPgpResult.Result(decryptedPrivateKeyJson(fingerprint = OTHER_FINGERPRINT)))

            interactor.fetchAndSaveMetadataKeys()

            val persistedKeys = persistedKeys()
            assertThat(persistedKeys).hasSize(1)
            assertThat(persistedKeys.single().metadataPrivateKeys).isEmpty()
        }

    @Test
    fun `private key not belonging to its metadata key is dropped`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PRIVATE_KEY)).thenReturn(OpenPgpResult.Result(OTHER_FINGERPRINT))
            whenever(openPgp.decryptMessageArmored(any(), any(), any()))
                .thenReturn(OpenPgpResult.Result(decryptedPrivateKeyJson(fingerprint = OTHER_FINGERPRINT)))

            interactor.fetchAndSaveMetadataKeys()

            assertThat(persistedKeys().single().metadataPrivateKeys).isEmpty()
        }

    @Test
    fun `private key with invalid object type is dropped`() =
        runTest {
            whenever(openPgp.decryptMessageArmored(any(), any(), any()))
                .thenReturn(OpenPgpResult.Result(decryptedPrivateKeyJson(objectType = "OTHER")))

            interactor.fetchAndSaveMetadataKeys()

            assertThat(persistedKeys().single().metadataPrivateKeys).isEmpty()
        }

    @Test
    fun `missing passphrase maps to unauthenticated failure without persisting`() =
        runTest {
            whenever(passphraseMemoryCache.get()).thenReturn(PotentialPassphrase.PassphraseNotPresent())

            val result = interactor.fetchAndSaveMetadataKeys()

            assertIs<MetadataKeysInteractor.Output.Failure>(result)
            val state = result.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(state)
            assertThat(state.reason).isEqualTo(Passphrase)
            verify(rebuildMetadataKeysTablesUseCase, never()).execute(any())
        }

    private suspend fun persistedKeys(): List<ParsedMetadataKeyModel> {
        val captor = argumentCaptor<RebuildMetadataKeysTablesUseCase.Input>()
        verify(rebuildMetadataKeysTablesUseCase).execute(captor.capture())
        return captor.firstValue.metadataKeys
    }

    private fun metadataKey(
        fingerprint: String? = FINGERPRINT,
        expired: ZonedDateTime? = null,
        metadataPrivateKeys: List<MetadataPrivateKeyModel> =
            listOf(
                MetadataPrivateKeyModel(
                    id = PRIVATE_KEY_ID,
                    metadataKeyId = METADATA_KEY_ID,
                    userId = PRIVATE_KEY_USER_ID,
                    pgpMessage = PGP_MESSAGE,
                    created = TIMESTAMP,
                    createdBy = null,
                    modified = TIMESTAMP,
                    modifiedBy = null,
                ),
            ),
    ) = MetadataKeyModel(
        id = METADATA_KEY_ID,
        fingerprint = fingerprint,
        armoredKey = PUBLIC_KEY,
        modified = ZonedDateTime.parse(TIMESTAMP),
        expired = expired,
        deleted = null,
        metadataPrivateKeys = metadataPrivateKeys,
    )

    private fun decryptedPrivateKeyJson(
        fingerprint: String = FINGERPRINT,
        objectType: String = "PASSBOLT_METADATA_PRIVATE_KEY",
    ) = gson.toJson(
        DecryptedMetadataPrivateKeyJsonModel(
            objectType = objectType,
            armoredKey = PRIVATE_KEY,
            passphrase = "",
            fingerprint = fingerprint,
            domain = "https://passbolt.local",
        ),
    )

    private companion object {
        private const val USER_ID = "userId"
        private const val USER_PRIVATE_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK----- user"
        private const val PASSPHRASE = "passphrase"
        private const val PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK----- metadata"
        private const val PRIVATE_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK----- metadata"
        private const val PGP_MESSAGE = "-----BEGIN PGP MESSAGE-----"
        private const val FINGERPRINT = "03F60E958F4CB29DBE4BE4EB3BD91E325CC7D42C"
        private const val OTHER_FINGERPRINT = "0000000000000000000000000000000000000000"
        private const val TIMESTAMP = "2025-01-01T10:00:00+00:00"
        private val METADATA_KEY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val PRIVATE_KEY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222")
        private val PRIVATE_KEY_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333")
    }
}
