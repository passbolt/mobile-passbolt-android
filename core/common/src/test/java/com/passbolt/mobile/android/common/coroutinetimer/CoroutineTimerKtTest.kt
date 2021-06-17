package com.passbolt.mobile.android.common.coroutinetimer

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.ExperimentalTime

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
@ExperimentalTime
@ExperimentalCoroutinesApi
class CoroutineTimerKtTest {

    @Test
    fun `test if timer is ticking fine`() = runBlocking {
        val timer = timerFlow(TICK_COUNT, TIMER_TICK_MILLIS)
        val expectedTickCount = 10

        timer.test {
            repeat(expectedTickCount) { expectItem() }
            expectComplete()
        }
    }

    private companion object {
        private const val TIMER_DURATION_MILLIS = 1_000L
        private const val TIMER_TICK_MILLIS = 100L
        private const val TICK_COUNT = TIMER_DURATION_MILLIS / TIMER_TICK_MILLIS
    }
}