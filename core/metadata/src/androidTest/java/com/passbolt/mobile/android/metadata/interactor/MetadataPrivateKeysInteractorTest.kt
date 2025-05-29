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

package com.passbolt.mobile.android.metadata.interactor

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.metadata.test.R
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.metadata.usecase.GetTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.SaveTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
import com.proton.gopenpgp.crypto.Crypto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.time.ZonedDateTime
import java.util.UUID

class MetadataPrivateKeysInteractorTest : KoinTest {
    private lateinit var gracePrivateKey: ByteArray
    private lateinit var gracePublicKey: String
    private lateinit var adminPublicKey: String
    private lateinit var adminPrivateKey: String
    private lateinit var privateKeySignedByAdmin: ByteArray
    private lateinit var privateKeySignedByGrace: ByteArray

    private val metadataPrivateKeysInteractor: MetadataPrivateKeysInteractor by inject()
    private val openPgp by inject<OpenPgp>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testMetadataPrivateKeysInteractorModule)
        }

    @Before
    fun setUp() {
        getInstrumentation().context.resources.apply {
            gracePrivateKey = openRawResource(R.raw.grace_private_key).readBytes()
            gracePublicKey = String(openRawResource(R.raw.grace_public_key).readBytes())
            adminPublicKey = String(openRawResource(R.raw.admin_public_key).readBytes())
            adminPrivateKey = String(openRawResource(R.raw.admin_private_key).readBytes())
            privateKeySignedByAdmin = openRawResource(R.raw.message_signed_by_admin).readBytes()
            privateKeySignedByGrace = openRawResource(R.raw.message_signed_by_grace).readBytes()
        }

        mockMetadataKeysInteractor.stub {
            onBlocking { fetchAndSaveMetadataKeys() } doReturn MetadataKeysInteractor.Output.Success
        }
        mockUpdateMetadataPrivateKeyUseCase.stub {
            onBlocking { execute(any()) } doReturn UpdateMetadataPrivateKeyUseCase.Output.Success
        }

        mockGetLocalUserUseCase.stub {
            onBlocking { execute(GetLocalUserUseCase.Input(GRACE_USER_ID)) } doReturn
                GetLocalUserUseCase.Output(
                    UserModel(
                        id = GRACE_USER_ID,
                        userName = "grace@passbolt.com",
                        disabled = false,
                        gpgKey =
                            GpgKeyModel(
                                armoredKey = gracePublicKey,
                                fingerprint = "63452C7A0AE6FAE8C8C309640BD9E2409BC6A569",
                                bits = 4096,
                                uid = "Grace Hopper <grace@passbolt.com>",
                                keyId = "0BD9E2409BC6A569",
                                type = "RSA",
                                keyExpirationDate = ZonedDateTime.now().plusDays(1),
                                keyCreationDate = ZonedDateTime.now().minusDays(1),
                                id = "d7c9f849-71ba-5940-a3ca-ab26472c06fb",
                            ),
                        profile =
                            UserProfileModel(
                                username = "grace@passbolt.com",
                                firstName = "grace",
                                lastName = "Hopper",
                                avatarUrl = null,
                            ),
                    ),
                )
            onBlocking { execute(GetLocalUserUseCase.Input(ADMIN_USER_ID)) } doReturn
                GetLocalUserUseCase.Output(
                    UserModel(
                        id = ADMIN_USER_ID,
                        userName = "admin@passbolt.com",
                        disabled = false,
                        gpgKey =
                            GpgKeyModel(
                                armoredKey = adminPublicKey,
                                fingerprint = "0C1D1761110D1E33C9006D1A5B1B332ED06426D3",
                                bits = 4096,
                                uid = "Passbolt Default Admin <admin@passbolt.com>",
                                keyId = "5B1B332ED06426D3",
                                type = "RSA",
                                keyExpirationDate = ZonedDateTime.now().plusDays(1),
                                keyCreationDate = ZonedDateTime.now().minusDays(1),
                                id = "91d8a7fd-3ab3-5e98-a4a5-0d8694ff23b9",
                            ),
                        profile =
                            UserProfileModel(
                                username = "admin@passbolt.com",
                                firstName = "Passbolt",
                                lastName = "Admin",
                                avatarUrl = null,
                            ),
                    ),
                )
        }
        mockGetSelectedUserPrivateKeyUseCase.stub {
            on { execute(any()) } doReturn GetSelectedUserPrivateKeyUseCase.Output(String(gracePrivateKey))
        }
        mockPassphraseMemoryCache.stub {
            on { get() } doReturn PotentialPassphrase.Passphrase("grace@passbolt.com".toByteArray())
        }

        mockSaveTrustedMetadataKeyUseCase.stub {
            onBlocking { execute(any()) } doReturn Unit
        }
        mockDeleteTrustedMetadataKeyUseCase.stub {
            onBlocking { execute(any()) } doReturn Unit
        }
        mockGetSelectedAccountDataUseCase.stub {
            onBlocking { execute(any()) } doReturn
                GetSelectedAccountDataUseCase.Output(
                    firstName = "Grace",
                    lastName = "Hopper",
                    email = "grace@passbolt.com",
                    avatarUrl = null,
                    url = "https://passbolt.local",
                    serverId = GRACE_USER_ID,
                    label = "grace",
                    role = "role",
                )
        }
    }

    @After
    fun tearDown() {
        reset(mockSaveTrustedMetadataKeyUseCase, mockUpdateMetadataPrivateKeyUseCase)
    }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsNoKeyServerSideAndNoKeyLocally() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn emptyList()
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn GetTrustedMetadataKeyUseCase.Output.NoTrustedKey
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            verify(mockSaveTrustedMetadataKeyUseCase, never()).execute(any())
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.NoMetadataKey::class.java)
        }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsNoKeyServerSideAndThereIsAKeyLocally() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn emptyList()
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    GetTrustedMetadataKeyUseCase.Output.TrustedKey(
                        id = UUID.randomUUID(),
                        userId = UUID.randomUUID(),
                        keyData = "",
                        passphrase = "",
                        created = ZonedDateTime.now(),
                        createdBy = UUID.randomUUID(),
                        modified = ZonedDateTime.now(),
                        modifiedBy = UUID.randomUUID(),
                        keyPgpMessage = "",
                        signingKeyFingerprint = "",
                        signatureCreationTimestampSeconds = 0,
                        signedUsername = "",
                        signedName = "",
                    )
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            verify(mockSaveTrustedMetadataKeyUseCase, never()).execute(any())
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.TrustedKeyDeleted::class.java)
        }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsBackendKeySignedByCurrentUserAndNoLocalKey() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    listOf(
                        ParsedMetadataKeyModel(
                            id = UUID.randomUUID(),
                            armoredKey = "",
                            fingerprint = "",
                            modified = ZonedDateTime.now(),
                            expired = null,
                            deleted = null,
                            metadataPrivateKeys =
                                listOf(
                                    ParsedMetadataPrivateKeyModel(
                                        id = UUID.randomUUID(),
                                        userId = UUID.fromString(GRACE_USER_ID),
                                        keyData = "",
                                        passphrase = "",
                                        created = ZonedDateTime.now(),
                                        createdBy = UUID.fromString(GRACE_USER_ID),
                                        modified = ZonedDateTime.now(),
                                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                                        pgpMessage = String(privateKeySignedByGrace),
                                        fingerprint = "",
                                        domain = "",
                                    ),
                                ),
                        ),
                    )
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn GetTrustedMetadataKeyUseCase.Output.NoTrustedKey
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            argumentCaptor<SaveTrustedMetadataKeyUseCase.Input> {
                verify(mockSaveTrustedMetadataKeyUseCase).execute(capture())
                assertSignedBy(
                    firstValue.keyPgpMessage,
                    String(gracePrivateKey),
                    "grace@passbolt.com",
                    gracePublicKey,
                )
                assertSignatureTime(
                    firstValue.keyPgpMessage,
                    String(gracePrivateKey),
                    "grace@passbolt.com",
                    gracePublicKey,
                    GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS,
                )
            }
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.KeyIsTrusted::class.java)
        }

    private suspend fun assertSignatureTime(
        pgpMessage: String,
        privateKey: String,
        passphrase: String,
        publicKey: String,
        expectedSignatureTime: Long,
    ) {
        val verifiedMessage =
            openPgp.verifySignature(
                privateKey,
                passphrase.toByteArray(),
                publicKey,
                pgpMessage.toByteArray(),
            )

        assertThat(verifiedMessage).isInstanceOf(OpenPgpResult.Result::class.java)
        assertThat((verifiedMessage as OpenPgpResult.Result).result.signatureCreationTimestampSeconds)
            .isEqualTo(expectedSignatureTime)
    }

    private suspend fun assertSignedBy(
        pgpMessage: String,
        privateKey: String,
        passphrase: String,
        publicKey: String,
    ) {
        val publicKeyFingerprint = Crypto.newKeyFromArmored(publicKey).fingerprint

        val verifiedMessage =
            openPgp.verifySignature(
                privateKey,
                passphrase.toByteArray(),
                publicKey,
                pgpMessage.toByteArray(),
            )

        assertThat(verifiedMessage).isInstanceOf(OpenPgpResult.Result::class.java)
        assertThat((verifiedMessage as OpenPgpResult.Result).result.signatureKeyFingerprint)
            .ignoringCase()
            .isEqualTo(publicKeyFingerprint)
    }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsBackendKeyNotSignedByCurrentUserAndNoLocalKey() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    listOf(
                        ParsedMetadataKeyModel(
                            id = UUID.randomUUID(),
                            armoredKey = "",
                            fingerprint = "",
                            modified = ZonedDateTime.now(),
                            expired = null,
                            deleted = null,
                            metadataPrivateKeys =
                                listOf(
                                    ParsedMetadataPrivateKeyModel(
                                        id = UUID.randomUUID(),
                                        userId = UUID.fromString(GRACE_USER_ID),
                                        keyData = "",
                                        passphrase = "",
                                        created = ZonedDateTime.now(),
                                        createdBy = UUID.fromString(ADMIN_USER_ID),
                                        modified = ZonedDateTime.now(),
                                        modifiedBy = UUID.fromString(ADMIN_USER_ID),
                                        pgpMessage = String(privateKeySignedByAdmin),
                                        fingerprint = "",
                                        domain = "",
                                    ),
                                ),
                        ),
                    )
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn GetTrustedMetadataKeyUseCase.Output.NoTrustedKey
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            argumentCaptor<SaveTrustedMetadataKeyUseCase.Input> {
                verify(mockSaveTrustedMetadataKeyUseCase).execute(capture())
                assertSignedBy(
                    firstValue.keyPgpMessage,
                    String(gracePrivateKey),
                    "grace@passbolt.com",
                    gracePublicKey,
                )
            }
            argumentCaptor<UpdateMetadataPrivateKeyUseCase.Input> {
                verify(mockUpdateMetadataPrivateKeyUseCase).execute(capture())
                assertSignedBy(
                    firstValue.privateKeyPgpMessage,
                    String(gracePrivateKey),
                    "grace@passbolt.com",
                    gracePublicKey,
                )
            }
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.KeyIsTrusted::class.java)
        }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsMatchingBackendAndLocalKey() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    listOf(
                        ParsedMetadataKeyModel(
                            id = UUID.randomUUID(),
                            armoredKey = "",
                            fingerprint = "",
                            modified = ZonedDateTime.now(),
                            expired = null,
                            deleted = null,
                            metadataPrivateKeys =
                                listOf(
                                    ParsedMetadataPrivateKeyModel(
                                        id = UUID.randomUUID(),
                                        userId = UUID.fromString(GRACE_USER_ID),
                                        keyData = "",
                                        passphrase = "",
                                        created = ZonedDateTime.now(),
                                        createdBy = UUID.fromString(GRACE_USER_ID),
                                        modified = ZonedDateTime.now(),
                                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                                        pgpMessage = String(privateKeySignedByGrace),
                                        fingerprint = "",
                                        domain = "",
                                    ),
                                ),
                        ),
                    )
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    GetTrustedMetadataKeyUseCase.Output.TrustedKey(
                        id = UUID.randomUUID(),
                        userId = UUID.fromString(GRACE_USER_ID),
                        keyData = "",
                        passphrase = "",
                        created = ZonedDateTime.now(),
                        createdBy = UUID.fromString(GRACE_USER_ID),
                        modified = ZonedDateTime.now(),
                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                        keyPgpMessage = String(privateKeySignedByGrace),
                        signingKeyFingerprint = "63452C7A0AE6FAE8C8C309640BD9E2409BC6A569",
                        signatureCreationTimestampSeconds = GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS,
                        signedUsername = "",
                        signedName = "",
                    )
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            verify(mockSaveTrustedMetadataKeyUseCase, never()).execute(any())
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.KeyIsTrusted::class.java)
        }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsBackendAndLocalKeyAndBackendKeyIsNotSigned() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    listOf(
                        ParsedMetadataKeyModel(
                            id = UUID.randomUUID(),
                            armoredKey = "",
                            fingerprint = "",
                            modified = ZonedDateTime.now(),
                            expired = null,
                            deleted = null,
                            metadataPrivateKeys =
                                listOf(
                                    ParsedMetadataPrivateKeyModel(
                                        id = UUID.randomUUID(),
                                        userId = UUID.fromString(GRACE_USER_ID),
                                        keyData = "",
                                        passphrase = "",
                                        created = ZonedDateTime.now(),
                                        createdBy = UUID.fromString(ADMIN_USER_ID),
                                        modified = ZonedDateTime.now(),
                                        modifiedBy = UUID.fromString(ADMIN_USER_ID),
                                        pgpMessage = String(privateKeySignedByAdmin),
                                        fingerprint = "",
                                        domain = "",
                                    ),
                                ),
                        ),
                    )
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    GetTrustedMetadataKeyUseCase.Output.TrustedKey(
                        id = UUID.randomUUID(),
                        userId = UUID.fromString(GRACE_USER_ID),
                        keyData = "",
                        passphrase = "",
                        created = ZonedDateTime.now(),
                        createdBy = UUID.fromString(GRACE_USER_ID),
                        modified = ZonedDateTime.now(),
                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                        keyPgpMessage = String(privateKeySignedByGrace),
                        signingKeyFingerprint = "63452C7A0AE6FAE8C8C309640BD9E2409BC6A569",
                        signatureCreationTimestampSeconds = GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS,
                        signedUsername = "",
                        signedName = "",
                    )
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            verify(mockSaveTrustedMetadataKeyUseCase, never()).execute(any())
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.NewKeyToTrust::class.java)
            val pgpMessage = (result as MetadataPrivateKeysInteractor.Output.NewKeyToTrust).metadataPrivateKey.pgpMessage
            assertSignedBy(
                pgpMessage,
                String(gracePrivateKey),
                "grace@passbolt.com",
                adminPublicKey,
            )
            assertSignatureTime(
                pgpMessage,
                String(gracePrivateKey),
                "grace@passbolt.com",
                adminPublicKey,
                ADMIN_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS,
            )
        }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsSignedBackendAndLocalKeyButBackendKeyIsOlder() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    listOf(
                        ParsedMetadataKeyModel(
                            id = UUID.randomUUID(),
                            armoredKey = "",
                            fingerprint = "",
                            modified = ZonedDateTime.now(),
                            expired = null,
                            deleted = null,
                            metadataPrivateKeys =
                                listOf(
                                    ParsedMetadataPrivateKeyModel(
                                        id = UUID.randomUUID(),
                                        userId = UUID.fromString(GRACE_USER_ID),
                                        keyData = "",
                                        passphrase = "",
                                        created = ZonedDateTime.now(),
                                        createdBy = UUID.fromString(GRACE_USER_ID),
                                        modified = ZonedDateTime.now(),
                                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                                        pgpMessage = String(privateKeySignedByGrace),
                                        fingerprint = "",
                                        domain = "",
                                    ),
                                ),
                        ),
                    )
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    GetTrustedMetadataKeyUseCase.Output.TrustedKey(
                        id = UUID.randomUUID(),
                        userId = UUID.fromString(GRACE_USER_ID),
                        keyData = "",
                        passphrase = "",
                        created = ZonedDateTime.now(),
                        createdBy = UUID.fromString(GRACE_USER_ID),
                        modified = ZonedDateTime.now(),
                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                        keyPgpMessage = String(privateKeySignedByGrace),
                        signingKeyFingerprint = "63452C7A0AE6FAE8C8C309640BD9E2409BC6A569",
                        signatureCreationTimestampSeconds = GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS + 1,
                        signedUsername = "",
                        signedName = "",
                    )
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            verify(mockSaveTrustedMetadataKeyUseCase, never()).execute(any())
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.NewKeyToTrust::class.java)
            val pgpMessage = (result as MetadataPrivateKeysInteractor.Output.NewKeyToTrust).metadataPrivateKey.pgpMessage
            assertSignedBy(
                pgpMessage,
                String(gracePrivateKey),
                "grace@passbolt.com",
                gracePublicKey,
            )
            assertSignatureTime(
                pgpMessage,
                String(gracePrivateKey),
                "grace@passbolt.com",
                gracePublicKey,
                GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS,
            )
        }

    @Test
    fun correctOutputShouldBeReturnedWhenThereIsSignedBackendAndLocalKeyButBackendKeyIsYounger() =
        runTest {
            mockGetLocalMetadataKeysUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    listOf(
                        ParsedMetadataKeyModel(
                            id = UUID.randomUUID(),
                            armoredKey = "",
                            fingerprint = "",
                            modified = ZonedDateTime.now(),
                            expired = null,
                            deleted = null,
                            metadataPrivateKeys =
                                listOf(
                                    ParsedMetadataPrivateKeyModel(
                                        id = UUID.randomUUID(),
                                        userId = UUID.fromString(GRACE_USER_ID),
                                        keyData = "",
                                        passphrase = "",
                                        created = ZonedDateTime.now(),
                                        createdBy = UUID.fromString(GRACE_USER_ID),
                                        modified = ZonedDateTime.now(),
                                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                                        pgpMessage = String(privateKeySignedByGrace),
                                        fingerprint = "",
                                        domain = "",
                                    ),
                                ),
                        ),
                    )
            }
            mockGetTrustedMetadataKeyUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    GetTrustedMetadataKeyUseCase.Output.TrustedKey(
                        id = UUID.randomUUID(),
                        userId = UUID.fromString(GRACE_USER_ID),
                        keyData = "",
                        passphrase = "",
                        created = ZonedDateTime.now(),
                        createdBy = UUID.fromString(GRACE_USER_ID),
                        modified = ZonedDateTime.now(),
                        modifiedBy = UUID.fromString(GRACE_USER_ID),
                        keyPgpMessage = String(privateKeySignedByGrace),
                        signingKeyFingerprint = "63452C7A0AE6FAE8C8C309640BD9E2409BC6A569",
                        signatureCreationTimestampSeconds = GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS - 1,
                        signedUsername = "",
                        signedName = "",
                    )
            }

            val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey()

            argumentCaptor<SaveTrustedMetadataKeyUseCase.Input> {
                verify(mockSaveTrustedMetadataKeyUseCase).execute(capture())
                assertSignedBy(
                    firstValue.keyPgpMessage,
                    String(gracePrivateKey),
                    "grace@passbolt.com",
                    gracePublicKey,
                )
                assertSignatureTime(
                    firstValue.keyPgpMessage,
                    String(gracePrivateKey),
                    "grace@passbolt.com",
                    gracePublicKey,
                    GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS,
                )
            }
            verify(mockUpdateMetadataPrivateKeyUseCase, never()).execute(any())
            assertThat(result).isInstanceOf(MetadataPrivateKeysInteractor.Output.KeyIsTrusted::class.java)
        }

    private companion object {
        private const val GRACE_USER_ID = "640ebc06-5ec1-5322-a1ae-6120ed2f3a74"
        private const val ADMIN_USER_ID = "d57c10f5-639d-5160-9c81-8a0c6c4ec856"

        private const val GRACE_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS = 1746451960L
        private const val ADMIN_SIGNED_PGP_MESSAGE_SIGNATURE_TIME_SECONDS = 1746514563L
    }
}
