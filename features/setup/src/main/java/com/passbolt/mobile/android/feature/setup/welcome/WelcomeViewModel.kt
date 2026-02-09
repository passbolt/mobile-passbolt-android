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

package com.passbolt.mobile.android.feature.setup.welcome

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.accounts.AccountKitParser
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.AcknowledgeDeviceRooted
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.ConnectToExistingAccount
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.DismissNoAccountExplanation
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.GoUp
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.ImportProfileManually
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.Initialize
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.SeeNoAccountExplanation
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.SelectedAccountKit
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToImportProfile
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToSummary
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToTransferDetails
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.ResultStatus
import com.passbolt.mobile.android.ui.ResultStatus.Failure
import com.passbolt.mobile.android.ui.ResultStatus.Success
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

internal class WelcomeViewModel(
    private val rootDetector: RootDetector,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val accountsInteractor: AccountsInteractor,
    private val accountKitParser: AccountKitParser,
) : SideEffectViewModel<WelcomeState, WelcomeSideEffect>(WelcomeState()),
    KoinComponent {
    fun onIntent(intent: WelcomeIntent) {
        when (intent) {
            is Initialize -> initialize(intent)
            GoUp -> emitSideEffect(NavigateUp)
            SeeNoAccountExplanation -> updateViewState { copy(showNoAccountExplanation = true) }
            ConnectToExistingAccount -> emitSideEffect(NavigateToTransferDetails)
            OpenHelpMenu -> updateViewState { copy(showHelpMenu = true) }
            DismissHelpMenu -> updateViewState { copy(showHelpMenu = false) }
            ImportProfileManually -> emitSideEffect(NavigateToImportProfile)
            AccessLogs -> emitSideEffect(NavigateToLogs)
            DismissNoAccountExplanation -> updateViewState { copy(showNoAccountExplanation = false) }
            is SelectedAccountKit -> accountKitSelected(intent.accountKit)
            AcknowledgeDeviceRooted -> updateViewState { copy(showDeviceRooted = false) }
        }
    }

    private fun initialize(intent: Initialize) {
        val shouldShowRootWarning = !getGlobalPreferencesUseCase.execute(Unit).isHideRootDialogEnabled && rootDetector.isDeviceRooted()
        updateViewState {
            copy(
                showBackNavigation = !intent.isTaskRoot,
                showDeviceRooted = shouldShowRootWarning,
            )
        }
    }

    private fun accountKitSelected(accountKit: String) {
        viewModelScope.launch {
            accountKitParser.parseAndVerify(
                accountKit,
                onSuccess = { injectPredefinedAccount(it) },
                onFailure = { emitSideEffect(NavigateToSummary(Failure(""))) },
            )
        }
    }

    private fun injectPredefinedAccount(accountSetupData: AccountSetupDataModel) {
        accountsInteractor.injectPredefinedAccountData(
            accountSetupData,
            onSuccess = { userId -> emitSideEffect(NavigateToSummary(Success(userId))) },
            onFailure = { failureType ->
                emitSideEffect(
                    NavigateToSummary(
                        when (failureType) {
                            ACCOUNT_ALREADY_LINKED -> ResultStatus.AlreadyLinked()
                            ERROR_NON_HTTPS_DOMAIN -> ResultStatus.HttpNotSupported()
                            ERROR_WHEN_SAVING_PRIVATE_KEY -> Failure(failureType.name)
                        },
                    ),
                )
            },
        )
    }
}
