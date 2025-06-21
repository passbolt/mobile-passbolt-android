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

package com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.ToggleDeveloperMode
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.ToggleHideRootWarning
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsScreenSideEffect.NavigateUp

internal class ExpertSettingsViewModel(
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
) : SideEffectViewModel<ExpertSettingsState, ExpertSettingsScreenSideEffect>(ExpertSettingsState()) {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: ExpertSettingsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            ToggleDeveloperMode -> toggleDeveloperMode()
            ToggleHideRootWarning -> toggleHideRootWarning()
        }
    }

    private fun loadInitialValues() {
        val globalPreferences = getGlobalPreferencesUseCase.execute(Unit)
        updateViewState {
            copy(
                isDeveloperModeChecked = globalPreferences.isDeveloperModeEnabled,
                isHideRootWarningEnabled = globalPreferences.isDeveloperModeEnabled,
                isHideRootWarningChecked = globalPreferences.isHideRootDialogEnabled,
            )
        }
    }

    private fun toggleDeveloperMode() {
        val isDeveloperModeChecked = !viewState.value.isDeveloperModeChecked

        if (isDeveloperModeChecked) {
            updateGlobalPreferencesUseCase.execute(
                UpdateGlobalPreferencesUseCase.Input(isDeveloperModeEnabled = true),
            )
            updateViewState {
                copy(
                    isDeveloperModeChecked = true,
                    isHideRootWarningEnabled = true,
                )
            }
        } else {
            updateGlobalPreferencesUseCase.execute(
                UpdateGlobalPreferencesUseCase.Input(
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = false,
                ),
            )
            updateViewState {
                copy(
                    isDeveloperModeChecked = false,
                    isHideRootWarningEnabled = false,
                    isHideRootWarningChecked = false,
                )
            }
        }
    }

    private fun toggleHideRootWarning() {
        val isHideRootWarningChecked = !viewState.value.isHideRootWarningChecked

        updateGlobalPreferencesUseCase.execute(
            UpdateGlobalPreferencesUseCase.Input(isHideRootDialogEnabled = isHideRootWarningChecked),
        )
        updateViewState {
            copy(isHideRootWarningChecked = isHideRootWarningChecked)
        }
    }
}
