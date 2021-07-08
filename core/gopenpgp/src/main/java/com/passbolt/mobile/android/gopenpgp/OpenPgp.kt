package com.passbolt.mobile.android.gopenpgp

import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.gopenpgp.exception.GopenPgpExceptionParser
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpException
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

    @Throws(OpenPgpException::class)
    suspend fun encryptSignMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        message: String
    ): String {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = ByteArray(passphrase.size) { passphrase[it] }

                val encrypted = Helper.encryptSignMessageArmored(
                    publicKey, privateKey, passphrase, message
                )
                passphraseCopy.erase()

                encrypted
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during encryptSignMessageArmored")
            throw gopenPgpExceptionParser.parseGopenPgpException(exception)
        } finally {
            Helper.freeOSMemory()
        }
    }

    @Throws(OpenPgpException::class)
    suspend fun decryptVerifyMessageArmored(
        publicKey: String,
        privateKey: String,
        passphrase: ByteArray,
        cipherText: String
    ): ByteArray {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = ByteArray(passphrase.size) { passphrase[it] }

                val decrypted = Helper.decryptVerifyMessageArmored(
                    publicKey, privateKey, passphrase, cipherText
                )
                val decryptedOutput = decrypted.toByteArray()

                passphraseCopy.erase()

                decryptedOutput
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during decryptVerifyMessageArmored")
            throw gopenPgpExceptionParser.parseGopenPgpException(exception)
        } finally {
            Helper.freeOSMemory()
        }
    }

    @Throws(OpenPgpException::class)
    suspend fun unlockKey(
        privateKey: String?,
        passphrase: ByteArray
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val passphraseCopy = ByteArray(passphrase.size) { passphrase[it] }

                val key = Key(privateKey)
                val unlockedKey = key.unlock(passphraseCopy)

                passphraseCopy.erase()

                unlockedKey.isUnlocked
            }
        } catch (exception: Exception) {
            Timber.e(exception, "There was an error during unlockKey")
            throw gopenPgpExceptionParser.parseGopenPgpException(exception)
        } finally {
            Helper.freeOSMemory()
        }
    }
}
