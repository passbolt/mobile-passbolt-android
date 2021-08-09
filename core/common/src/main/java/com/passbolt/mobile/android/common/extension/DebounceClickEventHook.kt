package com.passbolt.mobile.android.common.extension

import android.os.SystemClock
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.listeners.ClickEventHook

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
abstract class DebounceClickEventHook<T : GenericItem> : ClickEventHook<T>() {
    private var lastClickTime: Long = 0

    override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<T>, item: T) {
        if (shouldClickBeIgnored()) return else onDebounceClick(v, position, fastAdapter, item)

        lastClickTime = SystemClock.elapsedRealtime()
    }

    private fun shouldClickBeIgnored() = SystemClock.elapsedRealtime() - lastClickTime < DEBOUNCE_DELAY_MILLIS

    abstract fun onDebounceClick(v: View, position: Int, fastAdapter: FastAdapter<T>, item: T)
}

private const val DEBOUNCE_DELAY_MILLIS = 600L
