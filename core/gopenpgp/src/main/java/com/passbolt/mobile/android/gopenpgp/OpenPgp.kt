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

package com.passbolt.mobile.android.gopenpgp

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.extension.decodeHex
import com.passbolt.mobile.android.common.extension.encodeHex
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.gopenpgp.exception.GopenPgpExceptionParser
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpFailure
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.gopenpgp.model.CleartextSignatureVerification
import com.passbolt.mobile.android.gopenpgp.model.DecryptedMessageAndSessionKey
import com.passbolt.mobile.android.gopenpgp.model.VerifiedMessage
import com.proton.gopenpgp.constants.Constants.AES256
import com.proton.gopenpgp.crypto.Crypto
import com.proton.gopenpgp.crypto.Key
import com.proton.gopenpgp.crypto.PGPHandle
import com.proton.gopenpgp.crypto.VerifiedDataResult
import com.proton.gopenpgp.crypto.VerifyCleartextResult
import com.proton.gopenpgp.mobile.Mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant

class OpenPgp(
    private val pgpHandle: PGPHandle,
    private val gopenPgpExceptionParser: GopenPgpExceptionParser,
) {
    private var timeOffsetMillis: Long = 0L
    private var timeOffsetUncertaintyMillis: Long = 0L

    suspend fun encryptSignMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        message: String,
    ): OpenPgpResult<String> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val encryptionHandle =
                    pgpHandle
                        .encryptionWithTimeOffset()
                        .recipient(Crypto.newKeyFromArmored(publicKey))
                        .signingKey(Crypto.newPrivateKeyFromArmored(privateKey, passphraseCopy))
                        .new_()

                val encrypted =
                    encryptionHandle
                        .encrypt(
                            message.toByteArray(),
                        ).armor()

                OpenPgpResult.Result(encrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during encryptSignMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun encryptSignMessageArmored(
        privateKey: String,
        passphrase: ByteArray,
        message: String,
    ): OpenPgpResult<String> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val signingKey = Crypto.newPrivateKeyFromArmored(privateKey, passphraseCopy)
                val recipient = signingKey.toPublic()

                val encryptionHandle =
                    pgpHandle
                        .encryptionWithTimeOffset()
                        .signingKey(signingKey)
                        .recipient(recipient)
                        .new_()

                val encrypted =
                    encryptionHandle
                        .encrypt(
                            message.toByteArray(),
                        ).armor()

                OpenPgpResult.Result(encrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during encryptSignMessageArmored (with pk generation)")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptVerifyMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String,
    ): OpenPgpResult<String> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val decryptionHandle =
                    pgpHandle
                        .decryptionWithTimeOffset()
                        .decryptionKey(Crypto.newPrivateKeyFromArmored(privateKey, passphraseCopy))
                        .verificationKey(Crypto.newKeyFromArmored(publicKey))
                        .new_()

                val decryptionResult =
                    decryptionHandle
                        .decrypt(
                            cipherText.toByteArray(),
                            Crypto.Armor,
                        )

                enforceSignature { decryptionResult }

                val decrypted = decryptionResult.string()

                OpenPgpResult.Result(decrypted)
            }
        } catch (exception: SignatureVerificationException) {
            Timber.e(exception, "Signature verification failed during decryptVerifyMessageArmored")
            OpenPgpResult.Error(
                OpenPgpFailure.SignatureVerificationFailed(OpenPgpError(exception.cause?.message.orEmpty())),
            )
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptVerifyMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptVerifyMessageArmored(
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String,
    ): OpenPgpResult<String> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val decryptionKey = Crypto.newPrivateKeyFromArmored(privateKey, passphraseCopy)
                val verificationKey = Crypto.newKey(decryptionKey.publicKey)

                val decryptionHandle =
                    pgpHandle
                        .decryptionWithTimeOffset()
                        .decryptionKey(decryptionKey)
                        .verificationKey(verificationKey)
                        .new_()

                val decryptionResult =
                    decryptionHandle
                        .decrypt(
                            cipherText.toByteArray(),
                            Crypto.Armor,
                        )

                enforceSignature { decryptionResult }

                val decrypted = decryptionResult.string()

                OpenPgpResult.Result(decrypted)
            }
        } catch (exception: SignatureVerificationException) {
            Timber.e(exception, "Signature verification failed during decryptVerifyMessageArmored (with pk generation)")
            OpenPgpResult.Error(
                OpenPgpFailure.SignatureVerificationFailed(OpenPgpError(exception.cause?.message.orEmpty())),
            )
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptVerifyMessageArmored (with pk generation)")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun unlockKey(
        privateKey: String?,
        passphrase: ByteArray,
    ): OpenPgpResult<Boolean> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val unlockedKey = Key(privateKey).unlock(passphraseCopy)

                OpenPgpResult.Result(unlockedKey.isUnlocked)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during unlockKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptMessageArmored(
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String,
    ): OpenPgpResult<String> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val decryptionHandle =
                    pgpHandle
                        .decryptionWithTimeOffset()
                        .decryptionKey(Crypto.newPrivateKeyFromArmored(privateKey, passphraseCopy))
                        .new_()

                val decrypted =
                    decryptionHandle
                        .decrypt(
                            cipherText.toByteArray(),
                            Crypto.Armor,
                        ).string()

                OpenPgpResult.Result(decrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptSessionKey(
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String,
    ): OpenPgpResult<String> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val decryptionHandle =
                    pgpHandle
                        .decryptionWithTimeOffset()
                        .decryptionKey(Crypto.newPrivateKeyFromArmored(privateKey, passphraseCopy))
                        .new_()

                val decryptedSessionKey =
                    decryptionHandle.decryptSessionKey(
                        Crypto.newPGPMessageFromArmored(cipherText).keyPacket,
                    )

                OpenPgpResult.Result(decryptedSessionKey.key.encodeHex())
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptSessionKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    suspend fun generatePublicKey(privateKey: String): OpenPgpResult<String> =
        try {
            withContext(Dispatchers.Default) {
                OpenPgpResult.Result(
                    Crypto.newKeyFromArmored(privateKey).armoredPublicKey,
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during generatePublicKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }

    suspend fun getKeyFingerprint(key: String): OpenPgpResult<String> =
        try {
            withContext(Dispatchers.Default) {
                OpenPgpResult.Result(
                    Key(key).fingerprint,
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during getPrivateKeyFingerprint")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }

    suspend fun verifyClearTextSignature(
        armoredPublicKey: String,
        pgpMessage: ByteArray,
    ): OpenPgpResult<CleartextSignatureVerification> {
        return try {
            withContext(Dispatchers.Default) {
                val keyFingerprint = (getKeyFingerprint(armoredPublicKey) as OpenPgpResult.Result<String>).result

                val verificationHandle =
                    pgpHandle
                        .verificationWithTimeOffset()
                        .verificationKey(Crypto.newKeyFromArmored(armoredPublicKey))
                        .new_()

                val verificationResult = verificationHandle.verifyCleartext(pgpMessage)

                OpenPgpResult.Result(
                    CleartextSignatureVerification(
                        isSignatureVerified =
                            run {
                                enforceCleartextSignature { verificationResult }
                                true
                            },
                        message = String(verificationResult.cleartext()),
                        keyFingerprint = keyFingerprint,
                    ),
                )
            }
        } catch (exception: SignatureVerificationException) {
            Timber.e(exception, "Signature verification failed during verifyClearTextSignature")
            return OpenPgpResult.Error(
                OpenPgpFailure.SignatureVerificationFailed(OpenPgpError(exception.cause?.message.orEmpty())),
            )
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during verifyClearTextSignature")
            return OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    // this method is meant to be called in a loop
    // call freeMemory() after loop completes
    suspend fun decryptMessageArmoredWithSessionKey(
        sessionKeyHexString: String,
        message: String,
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.Default) {
                val pgpSessionKey =
                    Crypto.newSessionKeyFromToken(
                        sessionKeyHexString.decodeHex(),
                        SESSION_KEY_ALGORITHM,
                    )

                val decryptionHandle =
                    pgpHandle
                        .decryption()
                        .sessionKey(pgpSessionKey)
                        .new_()

                val result = decryptionHandle.decrypt(message.toByteArray(), Crypto.Auto)

                OpenPgpResult.Result(String(result.bytes()))
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptMessageArmoredWithSessionKey")
            return OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        }
    }

    // this method is meant to be called in a loop
    // call freeMemory() after loop completes
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun decryptMessageArmoredWithSessionKeyRetrieve(
        unlockedKey: Key,
        cipherText: String,
    ): OpenPgpResult<DecryptedMessageAndSessionKey> {
        return try {
            withContext(Dispatchers.Default) {
                val decryptionHandle =
                    pgpHandle
                        .decryptionWithTimeOffset()
                        .decryptionKey(unlockedKey)
                        .retrieveSessionKey()
                        .new_()

                val result =
                    decryptionHandle.decrypt(
                        cipherText.toByteArray(),
                        Crypto.Armor,
                    )

                OpenPgpResult.Result(
                    DecryptedMessageAndSessionKey(
                        decryptedMessage = result.string(),
                        sessionKeyHex = result.sessionKey().key.toHexString(),
                    ),
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptMessageArmoredWithSessionKey")
            return OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        }
    }

    suspend fun verifySignature(
        armoredPrivateKey: String,
        passphrase: ByteArray,
        armoredPublicKey: String,
        pgpMessage: ByteArray,
    ): OpenPgpResult<VerifiedMessage> {
        val passphraseCopy = passphrase.copyOf()
        return try {
            withContext(Dispatchers.Default) {
                val decryptionHandle =
                    pgpHandle
                        .decryptionWithTimeOffset()
                        .decryptionKey(Crypto.newPrivateKeyFromArmored(armoredPrivateKey, passphraseCopy))
                        .verificationKey(Crypto.newKeyFromArmored(armoredPublicKey))
                        .new_()

                val decryptionResult = decryptionHandle.decrypt(pgpMessage, Crypto.Armor)

                enforceSignature { decryptionResult }
                OpenPgpResult.Result(
                    VerifiedMessage(
                        decryptedMessage = String(decryptionResult.bytes()),
                        signatureCreationTimestampSeconds = decryptionResult.signatureCreationTime(),
                        signatureKeyFingerprint = decryptionResult.signedByKey().fingerprint,
                        signatureKeyHexKeyID = decryptionResult.signedByKey().hexKeyID,
                    ),
                )
            }
        } catch (exception: SignatureVerificationException) {
            Timber.e(exception, "Signature verification failed during verifySignature")
            return OpenPgpResult.Error(
                OpenPgpFailure.SignatureVerificationFailed(OpenPgpError(exception.cause?.message.orEmpty())),
            )
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during verifySignature")
            return OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            passphraseCopy.erase()
            Mobile.freeOSMemory()
        }
    }

    fun freeMemory() {
        Mobile.freeOSMemory()
    }

    /**
     * Sets time offset for all crypto operations for the session duration.
     */
    fun setTimeOffsetMillis(
        timeOffsetMs: Long,
        timeOffsetUncertaintyMs: Long,
    ) {
        timeOffsetMillis = timeOffsetMs
        timeOffsetUncertaintyMillis = timeOffsetUncertaintyMs
    }

    private fun PGPHandle.encryptionWithTimeOffset() =
        encryption()
            .encryptionTime(serverClockLowerBoundSeconds())
            .signTime(serverClockLowerBoundSeconds())

    private fun PGPHandle.decryptionWithTimeOffset() =
        decryption()
            .verifyTime(serverClockUpperBoundSeconds())

    private fun PGPHandle.verificationWithTimeOffset() =
        verify()
            .verifyTime(serverClockUpperBoundSeconds())

    private fun serverClockLowerBoundSeconds() =
        serverClockLowerBoundSeconds(Instant.now().toEpochMilli(), timeOffsetMillis, timeOffsetUncertaintyMillis)

    private fun serverClockUpperBoundSeconds() =
        serverClockUpperBoundSeconds(Instant.now().toEpochMilli(), timeOffsetMillis, timeOffsetUncertaintyMillis)

    /* IMPORTANT
     * gopenpgp is Go compiled to a native library via gomobile.
     * Setting a `.verificationKey(...)` on a decryption/verification handle does not make `decrypt()` fail
     * on a bad or missing signature - verification is only enforced by calling `signatureError()`
     */
    private fun enforceSignature(decryptionResult: () -> VerifiedDataResult) {
        try {
            decryptionResult().signatureError()
        } catch (exception: Exception) {
            throw SignatureVerificationException(exception)
        }
    }

    /* IMPORTANT
     * gopenpgp is Go compiled to a native library via gomobile.
     * Setting a `.verificationKey(...)` on a decryption/verification handle does not make `decrypt()` fail
     * on a bad or missing signature - verification is only enforced by calling `signatureError()`
     */
    private fun enforceCleartextSignature(cleartextResult: () -> VerifyCleartextResult) {
        try {
            cleartextResult().signatureError()
        } catch (exception: Exception) {
            throw SignatureVerificationException(exception)
        }
    }

    private class SignatureVerificationException(
        cause: Throwable,
    ) : Exception(cause)

    companion object {
        @VisibleForTesting
        const val SESSION_KEY_ALGORITHM = AES256

        @VisibleForTesting
        const val SERVER_TIME_RESOLUTION_SECS = 1L
        private const val MILLIS_PER_SECOND = 1_000L

        @VisibleForTesting
        fun serverClockLowerBoundSeconds(
            deviceTimeMillis: Long,
            timeOffsetMillis: Long,
            timeOffsetUncertaintyMillis: Long,
        ): Long = (deviceTimeMillis + timeOffsetMillis - timeOffsetUncertaintyMillis).floorDiv(MILLIS_PER_SECOND)

        @VisibleForTesting
        fun serverClockUpperBoundSeconds(
            deviceTimeMillis: Long,
            timeOffsetMillis: Long,
            timeOffsetUncertaintyMillis: Long,
        ): Long =
            (deviceTimeMillis + timeOffsetMillis + timeOffsetUncertaintyMillis).floorDiv(MILLIS_PER_SECOND) +
                SERVER_TIME_RESOLUTION_SECS
    }
}
