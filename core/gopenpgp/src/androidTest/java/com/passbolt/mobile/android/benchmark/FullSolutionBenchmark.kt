package com.passbolt.mobile.android.benchmark

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.passbolt.mobile.android.core.gopenpgp.test.R
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.GopenPgpExceptionParser
import com.passbolt.mobile.android.serializers.gson.strictTypeAdapters
import com.proton.gopenpgp.constants.Constants.AES256
import com.proton.gopenpgp.crypto.Crypto
import com.proton.gopenpgp.crypto.PGPHandle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.time.measureDurationForResult
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
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

class FullSolutionBenchmark : KoinTest {

    private val gson: Gson by inject()
    private lateinit var gracePrivateKey: String
    private lateinit var gracePublicKey: String
    private val cache = hashMapOf<UUID, String>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(module {
            factory { Crypto.pgp() }
            single { GopenPgpExceptionParser() }
            single { OpenPgp(gopenPgpExceptionParser = get(), pgpHandle = get()) }
            single {
                GsonBuilder()
                    .apply {
                        strictTypeAdapters.forEach {
                            registerTypeAdapter(it.key, it.value)
                        }
                    }
                    .create()
            }
        })
    }

    @Before
    fun setup() {
        getInstrumentation().context.resources.apply {
            gracePrivateKey = String(openRawResource(R.raw.grace_private_key).readBytes())
            gracePublicKey = String(openRawResource(R.raw.grace_public_key).readBytes())
        }
    }

    // benchmarks for encrypting and decrypting resources with PESK (Public Encrypted Session Key) method
    // and session key caching
    @Test
    fun test_encryptDecryptBySizePESK() {
        setOf(1, 10_000).forEach { size ->
            val resources = generateV5Resources(size)
            val encrypted = encryptResourceV5ListPESK(resources)
            val decrypted = decryptResourceV5ListPESK(encrypted)

            assertThat(decrypted.size).isEqualTo(resources.size)
            assertThat(decrypted).containsExactlyElementsIn(resources)
        }
    }

    // benchmarks for encrypting and decrypting resources with PESK (Public Encrypted Session Key) method
    // without session key caching
    @Test
    fun test_encryptDecryptNoCacheBySizePESK() {
        setOf(1, 10_000).forEach { size ->
            val resources = generateV5Resources(size)
            val encrypted = encryptResourceV5ListPESK(resources)
            cache.clear()
            val decrypted = decryptResourceV5ListPESK(encrypted)

            assertThat(decrypted.size).isEqualTo(resources.size)
            assertThat(decrypted).containsExactlyElementsIn(resources)
        }
    }

    private fun generateSessionKey() = Crypto.generateSessionKeyAlgo(SESSION_KEY_ALGO)

    @OptIn(ExperimentalStdlibApi::class)
    private fun encryptWithSessionKey(sessionKeyHex: String, message: ByteArray): ByteArray {
        val pgpHandle: PGPHandle by inject()

        val sessionKey = Crypto.newSessionKeyFromToken(
            sessionKeyHex.hexToByteArray(),
            SESSION_KEY_ALGO
        )

        return pgpHandle.encryption()
            .sessionKey(sessionKey)
            .new_()
            .encrypt(message)
            .bytes()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decryptWithSessionKey(sessionKeyHex: String, message: ByteArray): ByteArray {
        val pgpHandle: PGPHandle by inject()

        val sessionKey = Crypto.newSessionKeyFromToken(
            sessionKeyHex.hexToByteArray(),
            SESSION_KEY_ALGO
        )
        return pgpHandle.decryption()
            .sessionKey(sessionKey)
            .new_()
            .decrypt(message, Crypto.Auto)
            .bytes()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun encryptResourceV5ListPESK(list: List<ResourceV5>): List<ResourceV5> {
        val pgpHandle: PGPHandle by inject()
        val gracePk = Crypto.newKeyFromArmored(gracePublicKey)

        val (result, duration) = measureDurationForResult {
            list.map { resource ->
                // create a session key
                val sessionKey = generateSessionKey()

                // encrypt session key with a public key - PESK
                val encryptedSessionKey = pgpHandle
                    .encryption()
                    .recipient(gracePk)
                    .new_()
                    .encryptSessionKey(sessionKey)

                // encrypt message with session key - SEPID
                val encryptedMetadata = encryptWithSessionKey(
                    sessionKey.key.toHexString(),
                    resource.metadata.toByteArray()
                )

                // cache session key for future decryption
                cache[resource.id] = sessionKey.key.toHexString()

                // create split message with message packet and key packet
                val splitMessage = Crypto.newPGPSplitMessage(encryptedSessionKey, encryptedMetadata)
                val splitMessageArmored = splitMessage.armor()

                resource.copy(metadata = splitMessageArmored)
            }
        }

        println("encryptResourceV5ListPESK for ${list.size} resources took: $duration ms")

        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decryptResourceV5ListPESK(list: List<ResourceV5>): List<ResourceV5> {
        val pgpHandle: PGPHandle by inject()
        val gracePrivate = Crypto.newPrivateKeyFromArmored(
            gracePrivateKey,
            "grace@passbolt.com".toByteArray()
        )
        val decryptionHandle = pgpHandle.decryption()
            .decryptionKey(gracePrivate)
            .new_()

        val (result, duration) = measureDurationForResult {
            list.map { resource ->
                val pgpMessage = Crypto.newPGPMessageFromArmored(resource.metadata)

                // get from cache if present, otherwise decrypt session key
                val decryptedSessionKey: String = cache[resource.id] ?: let {
                    decryptionHandle.decryptSessionKey(pgpMessage.bytes()).key.toHexString()
                }

                // decrypt message with session key
                val decryptedMessage = decryptWithSessionKey(
                    decryptedSessionKey,
                    pgpMessage.dataPacket
                )

                val metadataString = String(decryptedMessage)
                val parsedMetadata = gson.fromJson(metadataString.reader(), ResourceV5Metadata::class.java)
                resource.copy(metadata = metadataString)
            }
        }

        println("decryptResourceV5ListPESK for ${list.size} resources took: $duration ms")

        return result
    }

    private fun generateV5Resources(count: Int) = (0 until count).map {
        ResourceV5(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            gson.toJson(
                ResourceV5Metadata(
                    "PASSBOLT_METADATA_V5",
                    "name",
                    "username",
                    listOf("uri", "uri2", "uri3"),
                    mapOf("jsonPath" to "xPath", "jsonPath2" to "xPath2", "jsonPath3" to "xPath3"),
                    listOf(
                        ResourceV5CustomField(UUID.randomUUID(), "key", "type"),
                        ResourceV5CustomField(UUID.randomUUID(), "key", "type"),
                        ResourceV5CustomField(UUID.randomUUID(), "key", "type"),
                    ),
                    ResourceV5Icon("type", "value")
                )
            ),
            "modified",
            "expired"
        )
    }

    private companion object {
        private const val SESSION_KEY_ALGO = AES256
    }
}
