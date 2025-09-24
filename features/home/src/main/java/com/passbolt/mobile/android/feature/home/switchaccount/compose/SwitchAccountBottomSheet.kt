package com.passbolt.mobile.android.feature.home.switchaccount.compose

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AccountDetails
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.StartUp
import com.passbolt.mobile.android.core.ui.compose.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.compose.dialogs.SignOutAlertDialog
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.Close
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.CloseSignOutDialog
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.Initialize
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.ManageAccounts
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.SeeCurrentAccountDetails
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.SignOut
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.SignOutConfirmed
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent.SwitchAccount
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountSideEffect.Dismiss
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountSideEffect.NavigateToAccountDetails
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountSideEffect.NavigateToManageAccounts
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountSideEffect.NavigateToSignInForAccount
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountSideEffect.NavigateToStartup
import com.passbolt.mobile.android.feature.home.switchaccount.recycler.SwitchAccountAccountsList
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchAccountBottomSheet(
    appContext: AppContext,
    onDismissRequest: () -> Unit,
    viewModel: SwitchAccountViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    viewModel.onIntent(Initialize(appContext))

    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current

    SwitchAccountBottomSheet(
        onIntent = viewModel::onIntent,
        onDismissRequest = onDismissRequest,
        state = state,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            Dismiss -> onDismissRequest()
            NavigateToAccountDetails -> {
                onDismissRequest()
                navigator.startNavigationActivity(context, AccountDetails)
            }
            is NavigateToSignInForAccount -> {
                onDismissRequest()
                if (sideEffect.appContext == AppContext.APP) {
                    activity?.finishAffinity()
                }
                navigator.startNavigationActivity(context, StartUp(sideEffect.appContext))
            }
            is NavigateToStartup -> {
                onDismissRequest()
                activity?.finishAffinity()
                navigator.startNavigationActivity(context, StartUp(sideEffect.appContext))
            }
            NavigateToManageAccounts -> {
                onDismissRequest()
                navigator.startNavigationActivity(context, NavigationActivity.ManageAccounts)
            }
        }
    }

    if (state.showSignOutDialog) {
        SignOutAlertDialog(
            isVisible = true,
            onDismiss = { viewModel.onIntent(CloseSignOutDialog) },
            onSignOutConfirm = { viewModel.onIntent(SignOutConfirmed) },
        )
    }

    ProgressDialog(isVisible = state.showProgress)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchAccountBottomSheet(
    onIntent: (SwitchAccountIntent) -> Unit,
    onDismissRequest: () -> Unit,
    state: SwitchAccountState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BottomSheetHeader(
                title = stringResource(LocalizationR.string.switch_account_title),
                onClose = { onIntent(Close) },
            )

            SwitchAccountAccountsList(
                accountsList = state.accountsList,
                onHeaderSeeDetailsClick = { onIntent(SeeCurrentAccountDetails) },
                onHeaderSignOutClick = { onIntent(SignOut) },
                onManageAccountsClick = { onIntent(ManageAccounts) },
                onAccountClick = { accountItem -> onIntent(SwitchAccount(accountItem)) },
            )
        }
    }
}
