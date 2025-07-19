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
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.dto.request.SessionKeyDto
import com.passbolt.mobile.android.dto.request.SessionKeysBundleDto
import com.passbolt.mobile.android.dto.response.DecryptedMetadataSessionKeysBundleModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.ZonedDateTime
import java.util.UUID

class SessionKeysCacheTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessionKeysBundleMerger = SessionKeysBundleMerger(TestCoroutineLaunchContext())
    private val sessionKeysCache = SessionKeysMemoryCache()

    @Test
    fun `most recently modified origin should be returned correctly`() =
        runTest {
            val keys1 = emptyList<SessionKeyDto>()
            val keys2 = emptyList<SessionKeyDto>()
            val bundle1Id = UUID.randomUUID()
            val bundle1 =
                DecryptedMetadataSessionKeysBundleModel(
                    id = bundle1Id,
                    created = ZonedDateTime.now(),
                    modified = ZonedDateTime.now(),
                    bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys1),
                )
            val bundle2Id = UUID.randomUUID()
            val bundle2 =
                DecryptedMetadataSessionKeysBundleModel(
                    id = bundle2Id,
                    created = ZonedDateTime.now().plusDays(1),
                    modified = ZonedDateTime.now().plusDays(1),
                    bundle = SessionKeysBundleDto("PASSBOLT_SESSION_KEYS", keys2),
                )

            val result = sessionKeysBundleMerger.merge(listOf(bundle1, bundle2))
            sessionKeysCache.value = result

            val mostRecentCacheItem = sessionKeysCache.findLatestModifiedOriginBundleData()
            assertThat(mostRecentCacheItem).isNotNull()
            assertThat(mostRecentCacheItem!!.originBundleId).isEqualTo(bundle2Id.toString())
            assertThat(mostRecentCacheItem.modifiedDate).isEqualTo(bundle2.modified)
        }
}
