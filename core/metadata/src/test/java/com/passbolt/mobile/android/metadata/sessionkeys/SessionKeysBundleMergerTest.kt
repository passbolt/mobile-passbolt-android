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

package com.passbolt.mobile.android.metadata.sessionkeys

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.dto.request.SessionKeyDto
import com.passbolt.mobile.android.dto.request.SessionKeysBundleDto
import com.passbolt.mobile.android.dto.response.DecryptedMetadataSessionKeysBundleModel
import com.passbolt.mobile.android.ui.SessionKeyIdentifier
import org.junit.Test
import java.time.ZonedDateTime
import java.util.UUID

class SessionKeysBundleMergerTest {
    private val sessionKeysBundleMerger = SessionKeysBundleMerger()

    @Test
    fun `single bundle should be kept in whole`() {
        val keys =
            listOf(
                SessionKeyDto("1", UUID.randomUUID(), "key1", ZonedDateTime.now().toString()),
                SessionKeyDto("2", UUID.randomUUID(), "key2", ZonedDateTime.now().toString()),
                SessionKeyDto("3", UUID.randomUUID(), "key3", ZonedDateTime.now().toString()),
            )
        val bundle =
            DecryptedMetadataSessionKeysBundleModel(
                id = UUID.randomUUID(),
                created = ZonedDateTime.now(),
                modified = ZonedDateTime.now(),
                bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys),
            )

        val result = sessionKeysBundleMerger.merge(listOf(bundle))

        assertThat(result.keys).hasSize(keys.size)
        assertThat(result.keys.keys).containsExactly(
            SessionKeyIdentifier("1", keys[0].foreignId),
            SessionKeyIdentifier("2", keys[1].foreignId),
            SessionKeyIdentifier("3", keys[2].foreignId),
        )
        assertThat(result.keys.map { it.value.sessionKey }).containsExactly("key1", "key2", "key3")
    }

    @Test
    fun `single bundle with duplicated models should be chosen by modified date`() {
        val sameForignModelId = UUID.randomUUID()
        val keys =
            listOf(
                SessionKeyDto("1", UUID.randomUUID(), "key1", ZonedDateTime.now().toString()),
                SessionKeyDto("2", UUID.randomUUID(), "key2", ZonedDateTime.now().toString()),
                SessionKeyDto("3", sameForignModelId, "key3", ZonedDateTime.now().toString()),
                SessionKeyDto("3", sameForignModelId, "key4", ZonedDateTime.now().minusDays(1).toString()),
            )
        val bundle =
            DecryptedMetadataSessionKeysBundleModel(
                id = UUID.randomUUID(),
                created = ZonedDateTime.now(),
                modified = ZonedDateTime.now(),
                bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys),
            )

        val result = sessionKeysBundleMerger.merge(listOf(bundle))

        assertThat(result.keys).hasSize(3)
        assertThat(result.keys.keys).containsExactly(
            SessionKeyIdentifier("1", keys[0].foreignId),
            SessionKeyIdentifier("2", keys[1].foreignId),
            SessionKeyIdentifier("3", keys[2].foreignId),
        )
        assertThat(result.keys.map { it.value.sessionKey }).containsExactly("key1", "key2", "key3")
    }

    @Test
    fun `multiple bundle should be kept in whole`() {
        val keys1 =
            listOf(
                SessionKeyDto("1", UUID.randomUUID(), "key1", ZonedDateTime.now().toString()),
                SessionKeyDto("2", UUID.randomUUID(), "key2", ZonedDateTime.now().toString()),
                SessionKeyDto("3", UUID.randomUUID(), "key3", ZonedDateTime.now().toString()),
            )
        val keys2 =
            listOf(
                SessionKeyDto("4", UUID.randomUUID(), "key4", ZonedDateTime.now().toString()),
                SessionKeyDto("5", UUID.randomUUID(), "key5", ZonedDateTime.now().toString()),
                SessionKeyDto("6", UUID.randomUUID(), "key6", ZonedDateTime.now().toString()),
            )
        val bundle1 =
            DecryptedMetadataSessionKeysBundleModel(
                id = UUID.randomUUID(),
                created = ZonedDateTime.now(),
                modified = ZonedDateTime.now(),
                bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys1),
            )
        val bundle2 =
            DecryptedMetadataSessionKeysBundleModel(
                id = UUID.randomUUID(),
                created = ZonedDateTime.now(),
                modified = ZonedDateTime.now(),
                bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys2),
            )

        val result = sessionKeysBundleMerger.merge(listOf(bundle1, bundle2))

        assertThat(result.keys).hasSize((keys1 + keys2).size)
        assertThat(result.keys.keys).containsExactly(
            SessionKeyIdentifier("1", keys1[0].foreignId),
            SessionKeyIdentifier("2", keys1[1].foreignId),
            SessionKeyIdentifier("3", keys1[2].foreignId),
            SessionKeyIdentifier("4", keys2[0].foreignId),
            SessionKeyIdentifier("5", keys2[1].foreignId),
            SessionKeyIdentifier("6", keys2[2].foreignId),
        )
        assertThat(result.keys.map { it.value.sessionKey }).containsExactly(
            "key1",
            "key2",
            "key3",
            "key4",
            "key5",
            "key6",
        )
    }

    @Test
    fun `multiple bundle with duplicated models should be chosen by modified date`() {
        val sameForeignModelId = UUID.randomUUID()
        val keys1 =
            listOf(
                SessionKeyDto("1", UUID.randomUUID(), "key1", ZonedDateTime.now().toString()),
                SessionKeyDto("2", sameForeignModelId, "key2", ZonedDateTime.now().minusDays(1).toString()),
                SessionKeyDto("3", UUID.randomUUID(), "key3", ZonedDateTime.now().toString()),
            )
        val keys2 =
            listOf(
                SessionKeyDto("4", UUID.randomUUID(), "key4", ZonedDateTime.now().toString()),
                SessionKeyDto("2", sameForeignModelId, "key5", ZonedDateTime.now().toString()),
                SessionKeyDto("6", UUID.randomUUID(), "key6", ZonedDateTime.now().toString()),
            )
        val bundle1 =
            DecryptedMetadataSessionKeysBundleModel(
                id = UUID.randomUUID(),
                created = ZonedDateTime.now(),
                modified = ZonedDateTime.now(),
                bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys1),
            )
        val bundle2 =
            DecryptedMetadataSessionKeysBundleModel(
                id = UUID.randomUUID(),
                created = ZonedDateTime.now(),
                modified = ZonedDateTime.now(),
                bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys2),
            )

        val result = sessionKeysBundleMerger.merge(listOf(bundle1, bundle2))

        assertThat(result.keys).hasSize(5)
        assertThat(result.keys.keys).containsExactly(
            SessionKeyIdentifier("1", keys1[0].foreignId),
            SessionKeyIdentifier("2", keys2[1].foreignId),
            SessionKeyIdentifier("3", keys1[2].foreignId),
            SessionKeyIdentifier("4", keys2[0].foreignId),
            SessionKeyIdentifier("6", keys2[2].foreignId),
        )
        assertThat(result.keys.map { it.value.sessionKey }).containsExactly(
            "key1",
            "key3",
            "key4",
            "key5",
            "key6",
        )
    }
}
