package com.passbolt.mobile.android.gopenpgp

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.extension.encodeHex
import com.passbolt.mobile.android.core.gopenpgp.test.R
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.proton.gopenpgp.crypto.Crypto
import com.proton.gopenpgp.crypto.PGPSplitMessage
import com.proton.gopenpgp.crypto.PlainMessage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.assertTrue

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

class OpenPgpTest : KoinTest {

    private val openPgp: OpenPgp by inject()

    private lateinit var gracePrivateKey: ByteArray
    private lateinit var gracePublicKey: String
    private lateinit var adminPublicKey: String
    private lateinit var pgpMessageSignedByAdmin: ByteArray
    private lateinit var pgpMessageSignedByGrace: ByteArray

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testOpenPgpModule)
    }

    @Before
    fun setup() {
        getInstrumentation().context.resources.apply {
            gracePrivateKey = openRawResource(R.raw.grace_private_key).readBytes()
            gracePublicKey = String(openRawResource(R.raw.grace_public_key).readBytes())
            adminPublicKey = String(openRawResource(R.raw.admin_public_key).readBytes())
            pgpMessageSignedByAdmin = openRawResource(R.raw.message_signed_by_admin).readBytes()
            pgpMessageSignedByGrace = openRawResource(R.raw.message_signed_by_grace).readBytes()
        }
    }

    @Test
    fun test_messageEncryptionDecryption() = runBlocking {
        val encrypted = openPgp.encryptSignMessageArmored(
            gracePublicKey,
            String(gracePrivateKey),
            GRACE_KEY_CORRECT_PASSPHRASE,
            PLAIN_MESSAGE
        )

        assertIsOpenPgpSuccessResult(encrypted)
        val decrypted = openPgp.decryptVerifyMessageArmored(
            gracePublicKey,
            String(gracePrivateKey),
            GRACE_KEY_CORRECT_PASSPHRASE,
            (encrypted as OpenPgpResult.Result).result
        )

        assertIsOpenPgpSuccessResult(decrypted)
        assertThat(String((decrypted as OpenPgpResult.Result).result)).isEqualTo(PLAIN_MESSAGE)
    }

    @Test
    fun test_messageEncryptionDecryptionWithPkGeneration() = runBlocking {
        val encrypted = openPgp.encryptSignMessageArmored(
            String(gracePrivateKey),
            GRACE_KEY_CORRECT_PASSPHRASE,
            PLAIN_MESSAGE
        )

        assertIsOpenPgpSuccessResult(encrypted)
        val decrypted = openPgp.decryptVerifyMessageArmored(
            String(gracePrivateKey),
            GRACE_KEY_CORRECT_PASSPHRASE,
            (encrypted as OpenPgpResult.Result).result
        )

        assertIsOpenPgpSuccessResult(decrypted)
        assertThat(String((decrypted as OpenPgpResult.Result).result)).isEqualTo(PLAIN_MESSAGE)
    }

    @Test
    fun test_messageDecryptionWithIncorrectPassphraseFailsWithCorrectException() = runBlocking {
        val encrypted = openPgp.encryptSignMessageArmored(
            gracePublicKey,
            String(gracePrivateKey),
            GRACE_KEY_CORRECT_PASSPHRASE,
            PLAIN_MESSAGE
        )
        assertIsOpenPgpSuccessResult(encrypted)

        val result = openPgp.decryptVerifyMessageArmored(
            gracePublicKey,
            String(gracePrivateKey),
            GRACE_KEY_WRONG_PASSPHRASE,
            (encrypted as OpenPgpResult.Result).result
        )

        assertIsOpenPgpErrorResult(result)
    }

    @Test
    fun test_messageEncryptionWithIncorrectPassphraseFailsWithCorrectException() = runBlocking {
        val result = openPgp.encryptSignMessageArmored(
            gracePublicKey,
            String(gracePrivateKey),
            GRACE_KEY_WRONG_PASSPHRASE,
            PLAIN_MESSAGE
        )

        assertIsOpenPgpErrorResult(result)
    }

    @Test
    fun test_keyShouldBeUnlockedWithCorrectPassphrase() = runBlocking {
        val isUnlocked = openPgp.unlockKey(
            String(gracePrivateKey),
            GRACE_KEY_CORRECT_PASSPHRASE
        )
        assertIsOpenPgpSuccessResult(isUnlocked)
        assertTrue((isUnlocked as OpenPgpResult.Result).result)
    }

    @Test
    fun test_keyShouldNotBeUnlockedWithWrongPassphrase() = runBlocking {
        val result = openPgp.unlockKey(
            String(gracePrivateKey),
            GRACE_KEY_WRONG_PASSPHRASE
        )

        assertIsOpenPgpErrorResult(result)
    }

    @Test
    fun test_messageSignatureShouldBeValidForCorrectData() = runBlocking {
        val result = openPgp.verifyClearTextSignature(
            adminPublicKey,
            pgpMessageSignedByAdmin
        )

        assertIsOpenPgpSuccessResult(result)
        val verificationResult = (result as OpenPgpResult.Result).result
        assertThat(verificationResult.isSignatureVerified).isTrue()
        assertThat(verificationResult.message).isEqualTo(PLAIN_MESSAGE)
    }

    @Test
    fun test_messageSignatureShouldBeInvalidForMessageSignedByOther() = runBlocking {
        val result = openPgp.verifyClearTextSignature(
            adminPublicKey,
            pgpMessageSignedByGrace
        )

        assertIsOpenPgpErrorResult(result)
    }

    @Test
    fun test_decryptSessionKeyShouldReturnCorrectKey() = runBlocking {
        val sessionKey = Crypto.generateSessionKeyAlgo(OpenPgp.SESSION_KEY_ALGORITHM)
        val sessionKeyHex = sessionKey.key.encodeHex()

        val message = sessionKey.encrypt(PlainMessage(PLAIN_MESSAGE))

        val gracePk = Crypto.newKeyFromArmored(gracePublicKey)
        val gracePkRing = Crypto.newKeyRing(gracePk)
        val encryptedSessionKey = gracePkRing.encryptSessionKey(sessionKey)

        val splitMessage = PGPSplitMessage(encryptedSessionKey, message)
        val splitMessageArmored = splitMessage.armored

        val decryptedSessionKey = openPgp.decryptSessionKey(
            String(gracePrivateKey), GRACE_KEY_CORRECT_PASSPHRASE, splitMessageArmored
        )

        assertIsOpenPgpSuccessResult(decryptedSessionKey)
        assertThat((decryptedSessionKey as OpenPgpResult.Result).result).isEqualTo(sessionKeyHex)
    }

    @Test
    fun test_decryptMessageArmoredWithSessionKeyShouldReturnMessage() = runBlocking {
        val sessionKey = Crypto.generateSessionKeyAlgo(OpenPgp.SESSION_KEY_ALGORITHM)

        val message = sessionKey.encrypt(PlainMessage(PLAIN_MESSAGE))

        val gracePk = Crypto.newKeyFromArmored(gracePublicKey)
        val gracePkRing = Crypto.newKeyRing(gracePk)
        val encryptedSessionKey = gracePkRing.encryptSessionKey(sessionKey)

        val splitMessage = PGPSplitMessage(encryptedSessionKey, message)
        val splitMessageArmored = splitMessage.armored

        val decryptedMessage = openPgp.decryptMessageArmored(
            String(gracePrivateKey), GRACE_KEY_CORRECT_PASSPHRASE, splitMessageArmored
        )

        assertIsOpenPgpSuccessResult(decryptedMessage)
        assertThat(String((decryptedMessage as OpenPgpResult.Result).result)).isEqualTo(PLAIN_MESSAGE)
    }

    private fun <T> assertIsOpenPgpSuccessResult(value: OpenPgpResult<T>) {
        assertThat(value).isInstanceOf(OpenPgpResult.Result::class.java)
    }

    private fun <T> assertIsOpenPgpErrorResult(result: OpenPgpResult<T>) {
        assertThat(result).isInstanceOf(OpenPgpResult.Error::class.java)
    }

    private companion object {
        private const val PLAIN_MESSAGE = "test message"
        private val GRACE_KEY_CORRECT_PASSPHRASE = "grace@passbolt.com".toByteArray()
        private val GRACE_KEY_WRONG_PASSPHRASE = "1111".toByteArray()
    }
}
