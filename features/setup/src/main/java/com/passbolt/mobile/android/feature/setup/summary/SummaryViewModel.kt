package com.passbolt.mobile.android.feature.setup.summary

import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.account.SaveAccountUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.database.usecase.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.AuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.GoBack
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.Initialize
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.PrimaryButtonAction
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToAppStart
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToFingerprintSetup
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToManageAccounts
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToWelcome
import com.passbolt.mobile.android.ui.ResultStatus.AlreadyLinked
import com.passbolt.mobile.android.ui.ResultStatus.Failure
import com.passbolt.mobile.android.ui.ResultStatus.HttpNotSupported
import com.passbolt.mobile.android.ui.ResultStatus.NoNetwork
import com.passbolt.mobile.android.ui.ResultStatus.Success

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
class SummaryViewModel(
    private val saveAccountUseCase: SaveAccountUseCase,
    private val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase,
    private val uuidProvider: UuidProvider,
) : SideEffectViewModel<SummaryState, SummarySideEffect>(
        SummaryState(),
    ) {
    fun onIntent(intent: SummaryIntent) {
        when (intent) {
            is Initialize -> updateViewState { copy(status = intent.status) }
            GoBack ->
                if (viewState.value.status is Success) {
                    updateViewState { copy(showSetupLeaveConfirmationDialog = true) }
                } else {
                    emitSideEffect(NavigateToWelcome)
                }
            ConfirmSetupLeave -> {
                updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
                emitSideEffect(NavigateToAppStart)
            }
            DismissSetupLeave -> updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
            OpenHelpMenu -> updateViewState { copy(showHelpMenu = true) }
            DismissHelpMenu -> updateViewState { copy(showHelpMenu = false) }
            AccessLogs -> emitSideEffect(NavigateToLogs)
            PrimaryButtonAction -> primaryButtonAction()
            AuthenticationSuccess -> authenticationSuccess()
        }
    }

    private fun primaryButtonAction() {
        viewState.value.status?.let { currentStatus ->
            when (currentStatus) {
                is AlreadyLinked -> emitSideEffect(NavigateToManageAccounts)
                is Success -> emitSideEffect(NavigateToSignIn(currentStatus.userId))
                is Failure -> emitSideEffect(NavigateToWelcome)
                is HttpNotSupported -> emitSideEffect(NavigateToWelcome)
                is NoNetwork -> emitSideEffect(NavigateToWelcome)
            }
        }
    }

    private fun authenticationSuccess() {
        viewState.value.status?.let {
            if (it is Success) {
                saveAccountUseCase.execute(UserIdInput(it.userId))
            }
        }
        val pass = uuidProvider.get()
        saveResourcesDatabasePassphraseUseCase.execute(
            SaveResourcesDatabasePassphraseUseCase.Input(pass),
        )
        emitSideEffect(NavigateToFingerprintSetup)
    }
}
