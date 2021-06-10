package com.passbolt.mobile.android.storage.cache.passphrase

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.passbolt.mobile.android.common.coroutines.timerFlow
import com.passbolt.mobile.android.common.extension.eraseArray
import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
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
    private val lifecycleOwner: LifecycleOwner
) : LifecycleObserver {

    private var value: PotentialPassphrase = PotentialPassphrase.PassphraseNotPresent

    private val timerFlow = timerFlow(TIMER_REPEAT_TIMES, TIMER_TICK_MILLIS)

    private val timerJob = SupervisorJob()
    private val timerScope = CoroutineScope(timerJob + coroutineLaunchContext.ui)

    // lifecycle observer remove/add methods need to be called on Main thread (even in Android Tests
    // TestDispatcher must not be injected here - Main dispatcher is obligatory
    private val lifecycleObserverJob = SupervisorJob()
    private val lifecycleObserverScope = CoroutineScope(lifecycleObserverJob + Dispatchers.Main)

    fun set(passphrase: CharArray) {
        clear()
        initializeObservers()
        value = PotentialPassphrase.Passphrase(passphrase)
        Timber.d("Passphrase cached")
    }

    fun get() = value

    private fun initializeObservers() {
        lifecycleObserverScope.launch {
            lifecycleOwner.lifecycle.addObserver(this@PassphraseMemoryCache)
        }
        timerScope.launch {
            timerFlow.collect { Timber.d("Timer $it/$TIMER_REPEAT_TIMES interval passed") }
            clear()
        }
    }

    private fun clear() {
        (value as? PotentialPassphrase.Passphrase).let {
            it?.passphrase?.eraseArray()
        }
        value = PotentialPassphrase.PassphraseNotPresent
        lifecycleObserverScope.launch {
            lifecycleOwner.lifecycle.removeObserver(this@PassphraseMemoryCache)
        }
        timerScope.coroutineContext.cancelChildren()
        lifecycleObserverScope.coroutineContext.cancelChildren()
        Timber.d("Cleared")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackground() {
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
