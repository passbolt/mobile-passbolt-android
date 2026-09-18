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

package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GopenPgpTimeUpdaterTest : KoinTest {
    private val mockOpenPgp = mock<OpenPgp>()
    private val gopenPgpTimeUpdater: GopenPgpTimeUpdater by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    factory { mockOpenPgp }
                    factoryOf(::GopenPgpTimeUpdater)
                },
            )
        }

    @Before
    fun setup() {
        reset(mockOpenPgp)
        whenever(mockOpenPgp.setTimeOffsetMillis(any(), any())).then { }
    }

    @Test
    fun `time should be synced if time delta is in range and device time is ahead`() {
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = (serverTime + GopenPgpTimeUpdater.TIME_DELTA_FOR_LOCAL_SYNC_SECS - 1) * MILLIS_PER_SECOND
        val requestDurationMillis = 0L

        val result = gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        assertThat(result).isEqualTo(GopenPgpTimeUpdater.Result.TIME_SYNCED)
    }

    @Test
    fun `time should be synced if time delta is in range and device time is behind`() {
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = (serverTime - GopenPgpTimeUpdater.TIME_DELTA_FOR_LOCAL_SYNC_SECS + 1) * MILLIS_PER_SECOND
        val requestDurationMillis = 0L

        val result = gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        assertThat(result).isEqualTo(GopenPgpTimeUpdater.Result.TIME_SYNCED)
    }

    @Test
    fun `time should not be synced if time delta is out of range and device time is ahead`() {
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = (serverTime + GopenPgpTimeUpdater.TIME_DELTA_FOR_LOCAL_SYNC_SECS + 1) * MILLIS_PER_SECOND
        val requestDurationMillis = 0L

        val result = gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        assertThat(result).isEqualTo(GopenPgpTimeUpdater.Result.TIME_DELTA_TOO_BIG_FOR_SYNC)
    }

    @Test
    fun `time should not be synced if time delta is out of range and device time is behind`() {
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = (serverTime - GopenPgpTimeUpdater.TIME_DELTA_FOR_LOCAL_SYNC_SECS - 1) * MILLIS_PER_SECOND
        val requestDurationMillis = 0L

        val result = gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        assertThat(result).isEqualTo(GopenPgpTimeUpdater.Result.TIME_DELTA_TOO_BIG_FOR_SYNC)
    }

    @Test
    fun `time should be synced on a slow connection when clocks agree`() {
        val requestDurationMillis = 30_000L
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = serverTime * MILLIS_PER_SECOND + requestDurationMillis / 2

        val result = gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        assertThat(result).isEqualTo(GopenPgpTimeUpdater.Result.TIME_SYNCED)
    }

    @Test
    fun `applies the fetch-time offset to gopenpgp when synced`() {
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = (serverTime + 3) * MILLIS_PER_SECOND
        val requestDurationMillis = 0L

        gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        verify(mockOpenPgp).setTimeOffsetMillis(-3_000, 0)
    }

    @Test
    fun `keeps sub-second request duration and clock phase in the offset`() {
        val serverTime = SERVER_TIME
        val deviceTimeAtFetchMillis = serverTime * MILLIS_PER_SECOND + 900
        val requestDurationMillis = 300L

        gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        verify(mockOpenPgp).setTimeOffsetMillis(-750, 150)
    }

    @Test
    fun `a reply held for seconds in transit widens the bounds instead of dragging the verify bound behind the server`() {
        val serverTime = SERVER_TIME
        val requestDurationMillis = 5_200L
        val deviceTimeAtFetchMillis = serverTime * MILLIS_PER_SECOND + requestDurationMillis

        gopenPgpTimeUpdater.updateTimeIfNeeded(serverTime, deviceTimeAtFetchMillis, requestDurationMillis)

        verify(mockOpenPgp).setTimeOffsetMillis(-2_600, 2_600)
        val deviceTimeAtVerifyMillis = (serverTime + 10) * MILLIS_PER_SECOND + 700
        assertThat(OpenPgp.serverClockUpperBoundSeconds(deviceTimeAtVerifyMillis, -2_600, 2_600)).isEqualTo(serverTime + 11)
        assertThat(OpenPgp.serverClockLowerBoundSeconds(deviceTimeAtVerifyMillis, -2_600, 2_600)).isEqualTo(serverTime + 5)
    }

    @Test
    fun `derived server clock bounds bracket the real server second wherever the reply was stamped in transit`() {
        var appliedOffsetMillis = 0L
        var appliedUncertaintyMillis = 0L
        whenever(mockOpenPgp.setTimeOffsetMillis(any(), any())).doAnswer {
            appliedOffsetMillis = it.getArgument(0)
            appliedUncertaintyMillis = it.getArgument(1)
        }

        for (deviceAheadMillis in -7_000L..7_000L step 500) {
            for (requestDurationMillis in listOf(200L, 900L, 1_500L, 3_900L)) {
                for (stampedAfterSendMillis in listOf(0L, requestDurationMillis / 2, requestDurationMillis)) {
                    for (fetchPhaseMillis in 0L until MILLIS_PER_SECOND step 100) {
                        val fetchArrivalMillis = SERVER_TIME * MILLIS_PER_SECOND + fetchPhaseMillis
                        val stampedAtDeviceMillis = fetchArrivalMillis - requestDurationMillis + stampedAfterSendMillis
                        val serverTimeSeconds = (stampedAtDeviceMillis - deviceAheadMillis).floorDiv(MILLIS_PER_SECOND)

                        val result =
                            gopenPgpTimeUpdater.updateTimeIfNeeded(serverTimeSeconds, fetchArrivalMillis, requestDurationMillis)

                        assertThat(result).isEqualTo(GopenPgpTimeUpdater.Result.TIME_SYNCED)
                        assertBoundsBracketRealServerSecondAtEveryVerifyPhase(
                            fetchArrivalMillis = fetchArrivalMillis,
                            deviceAheadMillis = deviceAheadMillis,
                            appliedOffsetMillis = appliedOffsetMillis,
                            appliedUncertaintyMillis = appliedUncertaintyMillis,
                            alignment =
                                "device ahead by $deviceAheadMillis ms, request $requestDurationMillis ms, " +
                                    "stamped $stampedAfterSendMillis ms after send, fetch phase $fetchPhaseMillis ms",
                        )
                    }
                }
            }
        }
    }

    private fun assertBoundsBracketRealServerSecondAtEveryVerifyPhase(
        fetchArrivalMillis: Long,
        deviceAheadMillis: Long,
        appliedOffsetMillis: Long,
        appliedUncertaintyMillis: Long,
        alignment: String,
    ) {
        for (verifyPhaseMillis in 0L until MILLIS_PER_SECOND step 100) {
            val deviceTimeMillis = fetchArrivalMillis + 2 * MILLIS_PER_SECOND + verifyPhaseMillis
            val realServerSecond = (deviceTimeMillis - deviceAheadMillis).floorDiv(MILLIS_PER_SECOND)
            val verifyAlignment = "$alignment, verify phase $verifyPhaseMillis ms"

            assertWithMessage(verifyAlignment)
                .that(OpenPgp.serverClockUpperBoundSeconds(deviceTimeMillis, appliedOffsetMillis, appliedUncertaintyMillis))
                .isAtLeast(realServerSecond)
            assertWithMessage(verifyAlignment)
                .that(OpenPgp.serverClockLowerBoundSeconds(deviceTimeMillis, appliedOffsetMillis, appliedUncertaintyMillis))
                .isAtMost(realServerSecond)
        }
    }

    private companion object {
        const val SERVER_TIME = 1_700_000_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
