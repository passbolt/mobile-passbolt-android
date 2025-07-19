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

package com.passbolt.mobile.android.logs

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.logs.LogsIntent.GoBack
import com.passbolt.mobile.android.logs.LogsIntent.ShareLogs
import com.passbolt.mobile.android.logs.LogsSideEffect.NavigateToLogsShareSheet
import com.passbolt.mobile.android.logs.LogsSideEffect.NavigateUp
import com.passbolt.mobile.android.logs.LogsSideEffect.ScrollLogsToLastLine
import com.passbolt.mobile.android.logs.reader.LogsReader
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

internal class LogsViewModel(
    private val logsReader: LogsReader,
) : SideEffectViewModel<LogsState, LogsSideEffect>(LogsState()),
    KoinComponent {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: LogsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            ShareLogs -> emitSideEffect(NavigateToLogsShareSheet(viewState.value.logFilePath))
        }
    }

    private fun loadInitialValues() {
        viewModelScope.launch {
            val logs = logsReader.getLogLines()
            val logsFilePath = logsReader.getLogFilePath()

            updateViewState {
                copy(logLines = logs, logFilePath = logsFilePath)
            }

            emitSideEffect(ScrollLogsToLastLine)
            emitSideEffect(NavigateToLogsShareSheet(logsFilePath))
        }
    }
}
