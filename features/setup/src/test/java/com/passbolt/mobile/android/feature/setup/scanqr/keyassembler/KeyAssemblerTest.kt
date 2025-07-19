package com.passbolt.mobile.android.feature.setup.scanqr.keyassembler

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.KeyAssembler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
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

@ExperimentalCoroutinesApi
class KeyAssemblerTest : KoinTest {
    private val keyAssembler: KeyAssembler by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(keyAssemblerModule)
        }

    @Test
    fun `key assembler should assemble the key correct`() =
        runTest {
            val keyBuffer =
                Buffer().apply {
                    write(KEY_JSON.toByteArray())
                }

            val key = keyAssembler.assemblePrivateKey(keyBuffer)
            assertThat(key).isEqualTo(ARMORED_KEY)
        }

    @Test
    fun `key assembler should not fail with additional data`() =
        runTest {
            val keyBuffer =
                Buffer().apply {
                    write(KEY_JSON_V2.toByteArray())
                }

            val key = keyAssembler.assemblePrivateKey(keyBuffer)
            assertThat(key).isEqualTo(ARMORED_KEY)
        }

    @Test
    fun `key assembler should fail when invalid uuids are met`() =
        runTest {
            val keyBuffer =
                Buffer().apply {
                    write(KEY_JSON_INVALID_UUID.toByteArray())
                }

            assertThrows(IllegalArgumentException::class.java) {
                keyAssembler.assemblePrivateKey(keyBuffer)
            }
        }

    private companion object {
        private const val ARMORED_KEY = "armoredKey"
        private const val FINGERPRINT = "fingerprint"
        private val USER_ID = UUID.randomUUID().toString()
        private val KEY_JSON =
            "{" +
                "\"armored_key\":\"$ARMORED_KEY\"," +
                "\"user_id\":\"$USER_ID\"," +
                "\"$FINGERPRINT\":\"fingerprint\"" +
                "}"
        private val KEY_JSON_V2 =
            "{" +
                "\"armored_key\":\"$ARMORED_KEY\"," +
                "\"user_id\":\"$USER_ID\"," +
                "\"fingerprint\":\"$FINGERPRINT\"," +
                "\"passphrase\":\"passphrase\"" +
                "}"
        private const val KEY_JSON_INVALID_UUID =
            "{" +
                "\"armored_key\":\"$ARMORED_KEY\"," +
                "\"user_id\":\"invalid_uuid\"," +
                "\"fingerprint\":\"$FINGERPRINT\"," +
                "\"passphrase\":\"passphrase\"" +
                "}"
    }
}
