package com.passbolt.mobile.android.gopenpgp

import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.gopenpgp.exception.GopenPgpExceptionParser
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.proton.Gopenpgp.crypto.Crypto
import com.proton.Gopenpgp.crypto.Key
import com.proton.Gopenpgp.helper.Helper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
class OpenPgp(private val gopenPgpExceptionParser: GopenPgpExceptionParser) {

    suspend fun encryptSignMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        message: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val encrypted = Helper.encryptSignMessageArmored(
                    publicKey, privateKey, passphrase, message
                )
                passphraseCopy.erase()

                OpenPgpResult.Result(encrypted)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during encryptSignMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Helper.freeOSMemory()
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

                val decrypted = Helper.decryptVerifyMessageArmored(
                    publicKey, privateKey, passphrase, cipherText
                )
                val decryptedOutput = decrypted.toByteArray()

                passphraseCopy.erase()

                OpenPgpResult.Result(decryptedOutput)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptVerifyMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Helper.freeOSMemory()
        }
    }

    suspend fun unlockKey(
        privateKey: String?,
        passphrase: ByteArray
    ): OpenPgpResult<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = passphrase.copyOf()

                val key = Key(privateKey)
                val unlockedKey = key.unlock(passphraseCopy)

                passphraseCopy.erase()

                OpenPgpResult.Result(unlockedKey.isUnlocked)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during unlockKey")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Helper.freeOSMemory()
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

                val decrypted = Helper.decryptMessageArmored(
                    privateKey, passphrase, cipherText
                )
                val decryptedOutput = decrypted.toByteArray()

                passphraseCopy.erase()

                OpenPgpResult.Result(decryptedOutput)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptMessageArmored")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Helper.freeOSMemory()
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
            Helper.freeOSMemory()
        }
    }

    suspend fun getPrivateKeyFingerprint(
        privateKey: String
    ): OpenPgpResult<String> {
        return try {
            withContext(Dispatchers.IO) {
                OpenPgpResult.Result(
                    Key(privateKey).fingerprint
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during getPrivateKeyFingerprint")
            OpenPgpResult.Error(gopenPgpExceptionParser.parseGopenPgpException(exception))
        } finally {
            Helper.freeOSMemory()
        }
    }
}
