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

package com.passbolt.mobile.android.feature.settings.screen

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.ConfirmSignOut
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.GoToAccounts
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.GoToAppSettings
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.GoToDebugLogs
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.GoToStartUp
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.GoToTermsAndLicenses
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.SignOut
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToAccounts
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToAppSettings
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToDebugLogs
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToStartUp
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToTermsAndLicenses
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val signOutUseCase: SignOutUseCase,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
) : SideEffectViewModel<SettingsState, SettingsSideEffect>(SettingsState()) {
    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            ConfirmSignOut -> signOut()
            GoToAccounts -> emitSideEffect(NavigateToAccounts)
            GoToAppSettings -> emitSideEffect(NavigateToAppSettings)
            GoToDebugLogs -> emitSideEffect(NavigateToDebugLogs)
            GoToTermsAndLicenses -> emitSideEffect(NavigateToTermsAndLicenses)
            GoToStartUp -> emitSideEffect(NavigateToStartUp)
            SignOut -> updateViewState { copy(isSignOutDialogVisible = true) }
            SettingsIntent.CancelSignOut -> updateViewState { copy(isSignOutDialogVisible = false) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            updateViewState { copy(isSignOutDialogVisible = false, isProgressDialogVisible = true) }
            // wait for full refresh to finish to minimize leaving db in an inconsistent state
            fullDataRefreshExecutor.awaitFinish()
            signOutUseCase.execute(Unit)
            updateViewState { copy(isProgressDialogVisible = false) }
            emitSideEffect(NavigateToStartUp)
        }
    }
}
