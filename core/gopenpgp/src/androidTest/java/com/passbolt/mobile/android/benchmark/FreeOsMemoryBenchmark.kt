package com.passbolt.mobile.android.benchmark

import android.os.Debug
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.gopenpgp.test.R
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.gopenpgp.testOpenPgpModule
import com.proton.gopenpgp.mobile.Mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime

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

class FreeOsMemoryBenchmark : KoinTest {
    private val openPgp: OpenPgp by inject()
    private lateinit var gracePrivateKey: String

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testOpenPgpModule)
        }

    @Before
    fun setup() {
        gracePrivateKey =
            String(
                getInstrumentation()
                    .context.resources
                    .openRawResource(R.raw.grace_private_key)
                    .readBytes(),
            )
    }

    @Test
    fun benchmark_sequentialDecrypts_smallPayload() =
        runBlocking {
            val payload = encrypt("small payload", smallPlaintext())
            compare("sequential x$SEQUENTIAL_DECRYPTS_SMALL, ${payload.label}") { afterEachDecrypt ->
                decryptSequentially(payload, SEQUENTIAL_DECRYPTS_SMALL, afterEachDecrypt)
            }
        }

    @Test
    fun benchmark_concurrentDecrypts_smallPayload() =
        runBlocking {
            val payload = encrypt("small payload", smallPlaintext())
            compare("concurrent x$CONCURRENCY, ${payload.label}") { afterEachDecrypt ->
                decryptConcurrently(payload, CONCURRENCY, afterEachDecrypt)
            }
        }

    @Test
    fun benchmark_sequentialDecrypts_bundleSizedPayload() =
        runBlocking {
            val payload = encrypt("bundle-sized payload", bundleSizedPlaintext())
            compare("sequential x$SEQUENTIAL_DECRYPTS_BUNDLE_SIZED, ${payload.label}") { afterEachDecrypt ->
                decryptSequentially(payload, SEQUENTIAL_DECRYPTS_BUNDLE_SIZED, afterEachDecrypt)
            }
        }

    @Test
    fun benchmark_concurrentDecrypts_bundleSizedPayload() =
        runBlocking {
            val payload = encrypt("bundle-sized payload", bundleSizedPlaintext())
            compare("concurrent x$CONCURRENCY, ${payload.label}") { afterEachDecrypt ->
                decryptConcurrently(payload, CONCURRENCY, afterEachDecrypt)
            }
        }

    private suspend fun compare(
        label: String,
        runVariant: suspend (afterEachDecrypt: () -> Unit) -> Unit,
    ) {
        runVariant(NO_OP)
        runVariant(FREE_OS_MEMORY)

        val noGc = mutableListOf<Sample>()
        val gcAfterEach = mutableListOf<Sample>()
        repeat(REPETITIONS) {
            noGc += sample { runVariant(NO_OP) }
            gcAfterEach += sample { runVariant(FREE_OS_MEMORY) }
        }

        println(
            "FreeOsMemoryBenchmark: $label, $REPETITIONS repetitions | " +
                "no GC: ${noGc.summary()} | GC after each: ${gcAfterEach.summary()}",
        )
    }

    private suspend fun sample(block: suspend () -> Unit): Sample {
        val duration = measureTime { block() }
        return Sample(duration, Debug.getPss() / KB_IN_MB)
    }

    private suspend fun decryptSequentially(
        payload: Payload,
        count: Int,
        afterEachDecrypt: () -> Unit,
    ) {
        repeat(count) { decryptOnce(payload, afterEachDecrypt) }
    }

    private suspend fun decryptConcurrently(
        payload: Payload,
        count: Int,
        afterEachDecrypt: () -> Unit,
    ) = coroutineScope {
        List(count) { async(Dispatchers.Default) { decryptOnce(payload, afterEachDecrypt) } }.awaitAll()
    }

    private suspend fun decryptOnce(
        payload: Payload,
        afterEachDecrypt: () -> Unit,
    ) {
        val decrypted = openPgp.decryptMessageArmored(gracePrivateKey, GRACE_PASSPHRASE, payload.encrypted)
        assertThat((decrypted as OpenPgpResult.Result).result).isEqualTo(payload.plaintext)
        afterEachDecrypt()
    }

    private suspend fun encrypt(
        label: String,
        plaintext: String,
    ): Payload {
        val encrypted = openPgp.encryptSignMessageArmored(gracePrivateKey, GRACE_PASSPHRASE, plaintext)
        return Payload(
            label = "$label (${plaintext.length / KB_IN_MB} KB)",
            plaintext = plaintext,
            encrypted = (encrypted as OpenPgpResult.Result).result,
        )
    }

    private fun smallPlaintext() = "{\"password\":\"benchmark\",\"description\":\"benchmark\"}"

    private fun bundleSizedPlaintext() =
        buildString {
            while (length < BUNDLE_SIZED_PAYLOAD_CHARS) {
                append("{\"foreign_id\":\"${UUID.randomUUID()}\",\"session_key\":\"9:${UUID.randomUUID()}\"},")
            }
        }

    private fun List<Sample>.summary(): String {
        val durations = map { it.duration }.sorted()
        val min = durations.first().millis()
        val median = durations[size / 2].millis()
        return "min=$min, median=$median, maxPss=${maxOf { it.pssMb }} MB"
    }

    private fun Duration.millis() = toString(DurationUnit.MILLISECONDS, decimals = 1)

    private class Payload(
        val label: String,
        val plaintext: String,
        val encrypted: String,
    )

    private class Sample(
        val duration: Duration,
        val pssMb: Long,
    )

    private companion object {
        private const val REPETITIONS = 10
        private const val SEQUENTIAL_DECRYPTS_SMALL = 40
        private const val SEQUENTIAL_DECRYPTS_BUNDLE_SIZED = 10
        private const val CONCURRENCY = 16
        private const val BUNDLE_SIZED_PAYLOAD_CHARS = 1_500_000
        private const val KB_IN_MB = 1024
        private val GRACE_PASSPHRASE = "grace@passbolt.com".toByteArray()
        private val NO_OP: () -> Unit = {}
        private val FREE_OS_MEMORY: () -> Unit = { Mobile.freeOSMemory() }
    }
}
