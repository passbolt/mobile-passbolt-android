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

package com.passbolt.mobile.android.feature.settings.screen.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AccountDetails
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.ManageAccounts
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.TransferAccount
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.KeyInspector
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsScreenSideEffect.NavigateToAccountDetails
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsScreenSideEffect.NavigateToKeyInspector
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsScreenSideEffect.NavigateToManageAccounts
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsScreenSideEffect.NavigateToTransferAccount
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsSettingsIntent.GoToAccountDetails
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsSettingsIntent.GoToKeyInspector
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsSettingsIntent.GoToManageAccounts
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsSettingsIntent.GoToTransferAccount
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun AccountsSettingsScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: AccountsSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    AccountsSettingsScreen(
        modifier = modifier,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToAccountDetails -> navigator.startNavigationActivity(context, AccountDetails)
            NavigateToKeyInspector -> navigator.navigateToKey(KeyInspector)
            NavigateToManageAccounts -> navigator.startNavigationActivity(context, ManageAccounts)
            NavigateToTransferAccount -> navigator.startNavigationActivity(context, TransferAccount)
            NavigateUp -> navigator.navigateBack()
        }
    }
}

@Composable
private fun AccountsSettingsScreen(
    onIntent: (AccountsSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_accounts),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_person),
            title = stringResource(LocalizationR.string.settings_accounts_account_details),
            onClick = { onIntent(GoToAccountDetails) },
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_key),
            title = stringResource(LocalizationR.string.settings_accounts_key_inspector),
            onClick = { onIntent(GoToKeyInspector) },
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_manage_accounts),
            title = stringResource(LocalizationR.string.settings_accounts_manage_accounts),
            onClick = { onIntent(GoToManageAccounts) },
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_transfer_account),
            title = stringResource(LocalizationR.string.settings_accounts_transfer_account),
            onClick = { onIntent(GoToTransferAccount) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountSettingsPreview() {
    AccountsSettingsScreen(
        onIntent = {},
        modifier = Modifier,
    )
}
