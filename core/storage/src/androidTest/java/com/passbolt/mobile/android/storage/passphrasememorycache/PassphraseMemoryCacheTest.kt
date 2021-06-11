package com.passbolt.mobile.android.storage.passphrasememorycache

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.dummy.TestActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject


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
class PassphraseMemoryCacheTest : KoinTest {

    private val passphraseMemoryCache: PassphraseMemoryCache by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testPassphraseMemoryCacheModule)
    }

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @Before
    fun setup() {
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun test_passphraseIsCachedForSetDuration() = runBlockingTest {
        passphraseMemoryCache.set(TEST_PASSPHRASE)

        // advance time on the timer thread to just before cache expiration
        testCoroutineLaunchContext.ui.advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS - 1)

        assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
        assertThat((passphraseMemoryCache.get() as PotentialPassphrase.Passphrase).passphrase)
            .isEqualTo(TEST_PASSPHRASE)
    }

    @Test
    fun test_cacheIsClearedAfterSetDuration() = runBlockingTest {
        passphraseMemoryCache.set(TEST_PASSPHRASE)

        // advance time on the timer thread to just after cache expiration
        testCoroutineLaunchContext.ui.advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS + 1)

        assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
    }

    @Test
    fun testCacheIsClearedAfterAppIsInBackground() = runBlocking {
        passphraseMemoryCache.set(TEST_PASSPHRASE)

        // start launcher app
        InstrumentationRegistry.getInstrumentation().context
            .startActivity(launcherIntent())

        delay(LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS)
        assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
    }

    @Test
    fun testCacheIsClearedAfterAppIsDestroyed() = runBlocking {
        passphraseMemoryCache.set(TEST_PASSPHRASE)

        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.DESTROYED)

        delay(LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS)
        assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
    }

    @Test
    fun testCacheTimeoutIsRenewedAfterNewPassphraseValueIsSet() = runBlocking {
        passphraseMemoryCache.set(TEST_PASSPHRASE)

        // advance time on the timer thread to just before cache expiration
        testCoroutineLaunchContext.ui.advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS - 1)
        // set new passphrase
        passphraseMemoryCache.set(TEST_PASSPHRASE)
        // advance time on the timer thread to just before cache expiration
        testCoroutineLaunchContext.ui.advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS - 1)

        delay(LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS)
        assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
    }

    private companion object {
        private val TEST_PASSPHRASE = "passphrase".toCharArray()
        private const val LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS = 1_000L

        fun launcherIntent() = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = FLAG_ACTIVITY_NEW_TASK
        }
    }
}
