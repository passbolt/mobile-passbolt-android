package com.passbolt.mobile.android.core.passphrasememorycache

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.passbolt.mobile.android.common.coroutinetimer.timerFlow
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
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

class PassphraseMemoryCache(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val lifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver {
    private var value: PotentialPassphrase = PotentialPassphrase.PassphraseNotPresent()

    private val timerFlow = timerFlow(TIMER_REPEAT_TIMES, TIMER_TICK_MILLIS)

    private val timerJob = SupervisorJob()
    private val timerScope = CoroutineScope(timerJob + coroutineLaunchContext.ui)
    private var currentTimerMillis: Long? = null

    // lifecycle observer remove/add methods need to be called on Main thread (even in Android Tests
    // TestDispatcher must not be injected here - Main dispatcher is obligatory
    private val lifecycleObserverJob = SupervisorJob()
    private val lifecycleObserverScope = CoroutineScope(lifecycleObserverJob + Dispatchers.Main)

    fun set(passphrase: ByteArray) {
        clear()
        initializeObservers()
        currentTimerMillis = TIMER_TICK_MILLIS
        value = PotentialPassphrase.Passphrase(passphrase.copyOf())
        Timber.d("Passphrase cached")
    }

    fun get() = value

    @Suppress("MagicNumber") // second has 1000 millis
    fun getSessionDurationSeconds() =
        currentTimerMillis?.let {
            (CACHE_EXPIRATION_MILLIS - it) / 1000
        }

    fun hasPassphrase() = value is PotentialPassphrase.Passphrase

    private fun initializeObservers() {
        lifecycleObserverScope.launch {
            lifecycleOwner.lifecycle.addObserver(this@PassphraseMemoryCache)
        }
        timerScope.launch {
            timerFlow.collect { currentTimerMillis = it * TIMER_TICK_MILLIS }
            clear()
        }
    }

    fun clear() {
        (value as? PotentialPassphrase.Passphrase).let {
            it?.passphrase?.erase()
        }
        value = PotentialPassphrase.PassphraseNotPresent()
        currentTimerMillis = null
        lifecycleObserverScope.launch {
            lifecycleOwner.lifecycle.removeObserver(this@PassphraseMemoryCache)
        }
        timerScope.coroutineContext.cancelChildren()
        lifecycleObserverScope.coroutineContext.cancelChildren()
        Timber.d("Passphrase cache cleared")
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("App went background")
        clear()
    }

    companion object {
        @VisibleForTesting
        const val CACHE_EXPIRATION_MILLIS = 1_000 * 60 * 5L

        private const val TIMER_TICK_MILLIS = 5_000L
        private const val TIMER_REPEAT_TIMES = CACHE_EXPIRATION_MILLIS / TIMER_TICK_MILLIS
    }
}
