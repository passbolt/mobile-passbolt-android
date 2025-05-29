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

package com.passbolt.mobile.android.feature.settings.screen.debuglogssettings

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsScreenSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.AccessLogs
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.OpenHelpWebsite
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.ToggleDebugLogs
import timber.log.Timber

internal class DebugLogsSettingsViewModel(
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val fileLoggingTree: FileLoggingTree,
) : SideEffectViewModel<DebugLogsSettingsState, DebugLogsScreenSideEffect>(DebugLogsSettingsState()) {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: DebugLogsSettingsIntent) {
        when (intent) {
            ToggleDebugLogs -> toggleDebugLogsIntent()
            AccessLogs -> emitSideEffect(NavigateToLogs)
            GoBack -> emitSideEffect(NavigateUp)
            OpenHelpWebsite -> emitSideEffect(DebugLogsScreenSideEffect.OpenHelpWebsite)
        }
    }

    private fun loadInitialValues() {
        val areLogsEnabled = getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled
        updateViewState {
            copy(
                areDebugLogsEnabled = areLogsEnabled,
                isAccessLogsEnabled = areLogsEnabled,
            )
        }
    }

    private fun toggleDebugLogsIntent() {
        val areLogsEnabled = !viewState.value.areDebugLogsEnabled
        if (areLogsEnabled) {
            if (!Timber.forest().contains(fileLoggingTree)) {
                Timber.plant(fileLoggingTree)
            }
        } else {
            if (Timber.forest().contains(fileLoggingTree)) {
                Timber.uproot(fileLoggingTree)
            }
        }
        updateGlobalPreferencesUseCase.execute(UpdateGlobalPreferencesUseCase.Input(areLogsEnabled))
        updateViewState {
            copy(
                areDebugLogsEnabled = areLogsEnabled,
                isAccessLogsEnabled = areLogsEnabled,
            )
        }
    }
}
