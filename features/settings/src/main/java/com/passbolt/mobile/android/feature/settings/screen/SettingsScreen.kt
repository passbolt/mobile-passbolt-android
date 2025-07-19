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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.dialogs.SignOutAlertDialog
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.CancelSignOut
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.ConfirmSignOut
import com.passbolt.mobile.android.feature.settings.screen.SettingsIntent.SignOut
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToAccounts
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToAppSettings
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToDebugLogs
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToStartUp
import com.passbolt.mobile.android.feature.settings.screen.SettingsSideEffect.NavigateToTermsAndLicenses
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun SettingsScreen(
    navigation: SettingsNavigation,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state.value,
        modifier = modifier,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToAccounts -> navigation.navigateToAccounts()
            NavigateToAppSettings -> navigation.navigateToAppSettingsLogs()
            NavigateToDebugLogs -> navigation.navigateToDebugLogs()
            NavigateToStartUp -> navigation.navigateToStartUp()
            NavigateToTermsAndLicenses -> navigation.navigateToTermsAndLicenses()
        }
    }
}

@Composable
private fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        Column {
            TitleAppBar(title = stringResource(LocalizationR.string.settings_title))

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_app_settings),
                title = stringResource(LocalizationR.string.settings_app_settings),
                onClick = { onIntent(SettingsIntent.GoToAppSettings) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_manage_accounts),
                title = stringResource(LocalizationR.string.settings_accounts),
                onClick = { onIntent(SettingsIntent.GoToAccounts) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_terms),
                title = stringResource(LocalizationR.string.settings_terms_and_licenses),
                onClick = { onIntent(SettingsIntent.GoToTermsAndLicenses) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_bug),
                title = stringResource(LocalizationR.string.settings_debug_logs),
                onClick = { onIntent(SettingsIntent.GoToDebugLogs) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_sign_out),
                title = stringResource(LocalizationR.string.settings_sign_out),
                onClick = { onIntent(SignOut) },
                opensInternally = false,
            )
        }

        ProgressDialog(isVisible = state.isProgressDialogVisible)

        SignOutAlertDialog(
            isVisible = state.isSignOutDialogVisible,
            onSignOutConfirm = { onIntent(ConfirmSignOut) },
            onDismiss = { onIntent(CancelSignOut) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    SettingsScreen(
        state = SettingsState(isSignOutDialogVisible = false),
        onIntent = {},
        modifier = Modifier,
    )
}
