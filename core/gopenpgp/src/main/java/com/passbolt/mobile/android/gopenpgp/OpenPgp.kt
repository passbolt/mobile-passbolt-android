package com.passbolt.mobile.android.gopenpgp

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.extension.decodeHex
import com.passbolt.mobile.android.common.extension.encodeHex
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.gopenpgp.exception.GopenPgpExceptionParser
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.gopenpgp.model.SignatureVerification
import com.proton.gopenpgp.constants.Constants.AES256
import com.proton.gopenpgp.crypto.Crypto
import com.proton.gopenpgp.crypto.Key
import com.proton.gopenpgp.crypto.PGPHandle
import com.proton.gopenpgp.mobile.Mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant

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
class OpenPgp(
    private val pgpHandle: PGPHandle,
    private val gopenPgpExceptionParser: GopenPgpExceptionParser
) {

    private var timeOffsetSeconds: Long = 0L

    suspend fun encryptSignMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        message: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val encryptionHandle = pgpHandle.encryptionWithTimeOffset()
                    .recipient(Crypto.newKeyFromArmored(publicKey))
                    .signingKey(Crypto.newPrivateKeyFromArmored(privateKey, passphrase))
                    .new_()

                val encrypted = encryptionHandle.encrypt(
                    message.toByteArray()
                ).armor()

                passphraseCopy.erase()

                OpenPgpResult.Result(encrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during encryptSignMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun encryptSignMessageArmored(
        privateKey: String,
        passphrase: ByteArray,
        message: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val recipientAndSigner = Crypto.newPrivateKeyFromArmored(privateKey, passphrase)

                val encryptionHandle = pgpHandle.encryptionWithTimeOffset()
                    .recipient(recipientAndSigner)
                    .signingKey(recipientAndSigner)
                    .new_()

                val encrypted = encryptionHandle.encrypt(
                    message.toByteArray()
                ).armor()

                passphraseCopy.erase()

                OpenPgpResult.Result(encrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during encryptSignMessageArmored (with pk generation)")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptVerifyMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String
    ): OpenPgpResult<ByteArray> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val decryptionHandle = pgpHandle.decryptionWithTimeOffset()
                    .decryptionKey(Crypto.newPrivateKeyFromArmored(privateKey, passphrase))
                    .verificationKey(Crypto.newKeyFromArmored(publicKey))
                    .new_()

                val decrypted = decryptionHandle.decrypt(
                    cipherText.toByteArray(), Crypto.Armor
                ).bytes()

                passphraseCopy.erase()

                OpenPgpResult.Result(decrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptVerifyMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptVerifyMessageArmored(
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String
    ): OpenPgpResult<ByteArray> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val decryptionKey = Crypto.newPrivateKeyFromArmored(privateKey, passphrase)
                val verificationKey = Crypto.newKey(decryptionKey.publicKey)

                val decryptionHandle = pgpHandle.decryptionWithTimeOffset()
                    .decryptionKey(decryptionKey)
                    .verificationKey(verificationKey)
                    .new_()

                val decrypted = decryptionHandle.decrypt(
                    cipherText.toByteArray(), Crypto.Armor
                ).bytes()

                passphraseCopy.erase()

                OpenPgpResult.Result(decrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptVerifyMessageArmored (with pk generation)")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun unlockKey(
        privateKey: String?,
        passphrase: ByteArray
    ): OpenPgpResult<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val unlockedKey = Key(privateKey).unlock(passphraseCopy)

                passphraseCopy.erase()

                OpenPgpResult.Result(unlockedKey.isUnlocked)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during unlockKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptMessageArmored(
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String
    ): OpenPgpResult<ByteArray> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val decryptionHandle = pgpHandle.decryptionWithTimeOffset()
                    .decryptionKey(Crypto.newPrivateKeyFromArmored(privateKey, passphrase))
                    .new_()

                val decrypted = decryptionHandle.decrypt(
                    cipherText.toByteArray(), Crypto.Armor
                ).bytes()

                passphraseCopy.erase()

                OpenPgpResult.Result(decrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptSessionKey(
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val decryptionHandle = pgpHandle.decryptionWithTimeOffset()
                    .decryptionKey(Crypto.newPrivateKeyFromArmored(privateKey, passphrase))
                    .new_()

                val decryptedSessionKey = decryptionHandle.decryptSessionKey(
                    Crypto.newPGPMessageFromArmored(cipherText).keyPacket
                )

                passphraseCopy.erase()

                OpenPgpResult.Result(decryptedSessionKey.key.encodeHex())
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptSessionKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun generatePublicKey(
        privateKey: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                OpenPgpResult.Result(
                    Crypto.newKeyFromArmored(privateKey).armoredPublicKey
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during generatePublicKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun getKeyFingerprint(key: String): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                OpenPgpResult.Result(
                    Key(key).fingerprint
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during getPrivateKeyFingerprint")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun verifyClearTextSignature(
        armoredPublicKey: String,
        pgpMessage: ByteArray
    ): OpenPgpResult<SignatureVerification> {
        return try {
            withContext(Dispatchers.IO) {
                val keyFingerprint = (getKeyFingerprint(armoredPublicKey) as OpenPgpResult.Result<String>).result

                val verificationHandle = pgpHandle.verificationWithTimeOffset()
                    .verificationKey(Crypto.newKeyFromArmored(armoredPublicKey))
                    .new_()

                val verificationResult = verificationHandle.verifyCleartext(pgpMessage)

                OpenPgpResult.Result(
                    SignatureVerification(
                        isSignatureVerified = try {
                            // signatureError() throws exception if signature is not valid
                            // returns unit if the signature is valid
                            verificationResult.signatureError()
                            true
                        } catch (e: Exception) {
                            // go to outer catch - signature is not valid
                            @Suppress("RethrowCaughtException")
                            throw e
                        },
                        message = String(verificationResult.cleartext()),
                        keyFingerprint = keyFingerprint
                    )
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during verifyClearTextSignature")
            return OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    suspend fun decryptMessageArmoredWithSessionKey(
        sessionKeyHexString: String,
        message: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                val pgpSessionKey = Crypto.newSessionKeyFromToken(
                    sessionKeyHexString.decodeHex(), SESSION_KEY_ALGORITHM
                )

                val decryptionHandle = pgpHandle.decryptionWithTimeOffset()
                    .sessionKey(pgpSessionKey)
                    .new_()

                val decrypted = decryptionHandle.decrypt(message.toByteArray(), Crypto.Armor)

                OpenPgpResult.Result(String(decrypted.bytes()))
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptMessageArmoredWithSessionKey")
            return OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Mobile.freeOSMemory()
        }
    }

    /**
     * Sets time offset for all crypto operations for the session duration.
     */
    fun setTimeOffsetSeconds(timeOffsetSec: Long) {
        timeOffsetSeconds = timeOffsetSec
    }

    private fun PGPHandle.encryptionWithTimeOffset() =
        encryption()
            .encryptionTime(Instant.now().epochSecond + timeOffsetSeconds)
            .signTime(Instant.now().epochSecond + timeOffsetSeconds)

    private fun PGPHandle.decryptionWithTimeOffset() =
        decryption()
            .verifyTime(Instant.now().epochSecond + timeOffsetSeconds)

    private fun PGPHandle.verificationWithTimeOffset() =
        verify()
            .verifyTime(Instant.now().epochSecond + timeOffsetSeconds)

    companion object {
        @VisibleForTesting
        const val SESSION_KEY_ALGORITHM = AES256
    }
}
