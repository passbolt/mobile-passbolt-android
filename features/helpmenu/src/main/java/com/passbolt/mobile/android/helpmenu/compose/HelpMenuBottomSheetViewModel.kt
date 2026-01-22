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

package com.passbolt.mobile.android.helpmenu.compose

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.AccessLogs
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.AccountKitRead
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.Close
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.DismissQrCodesDialog
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.ImportAccountKit
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.ImportProfileManually
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.Initialize
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.SeeWhyQrCodesExplanation
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.ToggleEnableLogs
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.VisitHelpWebsite
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.Dismiss
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NavigateToAccessLogs
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NavigateToImportAccountKit
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NavigateToImportProfileManually
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NotifyAccountKitRead
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.OpenHelpWebsite
import com.passbolt.mobile.android.ui.HelpMenuModel
import timber.log.Timber

class HelpMenuBottomSheetViewModel(
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
    private val fileLoggingTree: FileLoggingTree,
) : SideEffectViewModel<HelpMenuBottomSheetState, HelpMenuBottomSheetSideEffect>(HelpMenuBottomSheetState()) {
    init {
        initializeLogsState()
    }

    private fun initializeLogsState() {
        val areLogsEnabled = getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled
        updateViewState { copy(enableLogsSwitch = areLogsEnabled, accessLogsEnabled = areLogsEnabled) }
    }

    fun onIntent(intent: HelpMenuBottomSheetIntent) {
        when (intent) {
            Close -> emitSideEffect(Dismiss)
            SeeWhyQrCodesExplanation -> updateViewState { copy(showWhyQrCodesExplanationDialog = true) }
            DismissQrCodesDialog -> updateViewState { copy(showWhyQrCodesExplanationDialog = false) }
            AccessLogs -> {
                emitSideEffect(Dismiss)
                emitSideEffect(NavigateToAccessLogs)
            }
            ImportProfileManually -> {
                emitSideEffect(Dismiss)
                emitSideEffect(NavigateToImportProfileManually)
            }
            ImportAccountKit -> {
                emitSideEffect(Dismiss)
                emitSideEffect(NavigateToImportAccountKit)
            }
            VisitHelpWebsite -> {
                emitSideEffect(Dismiss)
                emitSideEffect(OpenHelpWebsite)
            }
            is AccountKitRead -> {
                emitSideEffect(Dismiss)
                emitSideEffect(NotifyAccountKitRead(intent.accountKit))
            }
            is ToggleEnableLogs -> handleEnableLogsToggled(intent.enabled)
            is Initialize -> initialize(intent.helpMenuModel)
        }
    }

    private fun initialize(helpMenuModel: HelpMenuModel) {
        updateViewState {
            copy(
                showImportProfileHelp = helpMenuModel.shouldShowImportProfile,
                showImportAccountKitHelp = helpMenuModel.shouldShowImportAccountKit,
                showScanQrCodesHelp = helpMenuModel.shouldShowShowQrCodesHelp,
            )
        }
    }

    private fun handleEnableLogsToggled(enabled: Boolean) {
        updateGlobalPreferencesUseCase.execute(UpdateGlobalPreferencesUseCase.Input(enabled))

        updateViewState {
            copy(
                enableLogsSwitch = enabled,
                accessLogsEnabled = enabled,
            )
        }

        if (enabled) {
            if (!Timber.forest().contains(fileLoggingTree)) {
                Timber.plant(fileLoggingTree)
            }
        } else {
            if (Timber.forest().contains(fileLoggingTree)) {
                Timber.uproot(fileLoggingTree)
            }
        }
    }
}
